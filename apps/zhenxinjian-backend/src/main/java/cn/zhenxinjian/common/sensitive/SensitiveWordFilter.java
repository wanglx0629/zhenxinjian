package cn.zhenxinjian.common.sensitive;

import cn.hutool.dfa.WordTree;
import cn.zhenxinjian.common.constant.CommonConstant;
import cn.zhenxinjian.common.constant.ExceptionConstant;
import cn.zhenxinjian.common.constant.ProjectConfigKeyConstant;
import cn.zhenxinjian.common.exception.BusinessException;
import cn.zhenxinjian.service.ConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 敏感词过滤组件（DFA 匹配，Hutool WordTree）
 * 词库 = 内置词库 − 排除词 + 追加词，统一小写；开关关闭或组件异常均 fail-open 放行；
 * 词库读取/配置读取失败仅记 ERROR 不阻塞启动
 * 作者: wanglx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveWordFilter {

    /** 内置词库文件（classpath，UTF-8，一行一词，# 开头为注释行） */
    private static final String WORDLIST_RESOURCE = "sensitive-words.txt";

    private final ConfigService configService;

    /** 匹配词树（volatile 保证 refresh 后对匹配线程可见） */
    private volatile WordTree wordTree = new WordTree();

    /** 过滤总开关（volatile；加载失败/开关关闭时放行） */
    private volatile boolean enabled = false;

    /**
     * 启动时重建词库（admin 修改 sensitive.filter.* 配置后由 Controller 再次触发）
     */
    @PostConstruct
    public void refresh() {
        try {
            boolean enabledNow = configService.getBoolean(
                    ProjectConfigKeyConstant.SENSITIVE_FILTER_ENABLED, true);
            Set<String> words = loadBuiltinWords();
            removeExcluded(words, configService.getStringList(
                    ProjectConfigKeyConstant.SENSITIVE_FILTER_EXCLUDE_WORDS));
            addExtra(words, configService.getStringList(
                    ProjectConfigKeyConstant.SENSITIVE_FILTER_EXTRA_WORDS));

            WordTree tree = new WordTree();
            tree.addWords(words);
            this.wordTree = tree;
            this.enabled = enabledNow;
            log.info("敏感词库刷新完成：开关 {}，词量 {}", enabledNow, words.size());
        } catch (Exception e) {
            // fail-open：不阻塞启动，过滤暂时关闭
            this.wordTree = new WordTree();
            this.enabled = false;
            log.error("敏感词库刷新失败，过滤暂时关闭（fail-open）", e);
        }
    }

    /**
     * 内容校验：命中敏感词抛统一业务异常；开关关闭/空白文本放行
     *
     * @param text 待校验文本（可空）
     */
    public void check(String text) {
        if (contains(text)) {
            throw new BusinessException(CommonConstant.CONTAINS_SENSITIVE_WORD_CODE,
                    ExceptionConstant.CONTAINS_SENSITIVE_WORD);
        }
    }

    /**
     * 是否命中敏感词（开关关闭/空白文本返回 false）
     *
     * @param text 待校验文本（可空）
     * @return true 表示命中
     */
    public boolean contains(String text) {
        if (!enabled || !StringUtils.hasText(text)) {
            return false;
        }
        // 文本与词统一小写，大小写不敏感；默认过滤停顿符（空格/标点），防拆字绕过
        return wordTree.isMatch(text.toLowerCase(Locale.ROOT));
    }

    /** 读取内置词库：跳过空白行与 # 注释行，统一小写 */
    private Set<String> loadBuiltinWords() throws Exception {
        Set<String> words = new HashSet<>();
        try (InputStream in = new ClassPathResource(WORDLIST_RESOURCE).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim();
                if (StringUtils.hasText(word) && !word.startsWith("#")) {
                    words.add(word.toLowerCase(Locale.ROOT));
                }
            }
        }
        return words;
    }

    /** 排除词差集（内置词库豁免，避免食物词等误杀） */
    private void removeExcluded(Set<String> words, List<String> excludeWords) {
        excludeWords.stream()
                .filter(StringUtils::hasText)
                .map(w -> w.trim().toLowerCase(Locale.ROOT))
                .forEach(words::remove);
    }

    /** 追加词并集（业务自定义补充） */
    private void addExtra(Set<String> words, List<String> extraWords) {
        extraWords.stream()
                .filter(StringUtils::hasText)
                .map(w -> w.trim().toLowerCase(Locale.ROOT))
                .forEach(words::add);
    }
}
