import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 执行器（臻心减本机工具）：按分号切分执行 SQL 文件，结果输出到 stdout
 *
 * 版本记账：文件名形如 change<N>_<描述>.sql 的迁移脚本自动纳入 schema_migrations 版本表管理——
 *   执行前先建版本表（IF NOT EXISTS）并查账，已应用则整体跳过；全部语句成功后回写记账。
 *   非 change 命名（验证查询等）不参与版本管理，直接执行。
 * 失败策略：任何语句报错立即终止（exit 2），不吞错、不记账。
 *
 * 凭据解析顺序：
 *   1. 命令行传入的 .properties 文件路径
 *   2. 环境变量 MYSQL_URL / MYSQL_USERNAME / MYSQL_PASSWORD
 * 推荐：tools\db\run-sql.cmd 会自动传入 db.local.properties（gitignored）
 *
 * 用法: java -cp <mysql-connector.jar>;<编译输出目录> SqlRunner <sql文件> [xx.properties]
 * 作者: wanglx
 */
public class SqlRunner {

    /** change 脚本命名：change<N>_<描述>.sql（N 即版本号） */
    private static final Pattern CHANGE_FILE = Pattern.compile("^change(\\d+)_.*\\.sql$");

    /** 版本记账表 DDL（与 sql/change0_schema_migrations.sql 一致，自带 IF NOT EXISTS 幂等） */
    private static final String VERSION_TABLE_DDL =
            "CREATE TABLE IF NOT EXISTS schema_migrations ("
                    + " version INT NOT NULL COMMENT '变更序号（change<N> 的 N，0=版本表自身）',"
                    + " script VARCHAR(128) NOT NULL COMMENT '脚本文件名',"
                    + " applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '应用时刻',"
                    + " PRIMARY KEY (version)"
                    + " ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL 迁移版本记账表：每个 change 脚本仅应用一次'";

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        String sqlFile = null;
        String propsPath = null;
        for (String a : args) {
            if (a.endsWith(".properties")) {
                propsPath = a;
            } else if (a.startsWith("--")) {
                System.out.println("未知选项: " + a + "（--allow-error 已移除，失败即终止）");
                System.exit(1);
            } else {
                sqlFile = a;
            }
        }
        if (sqlFile == null) {
            System.out.println("用法: SqlRunner <sql文件> [db.local.properties]");
            System.exit(1);
        }

        String url = prop(propsPath, "MYSQL_URL");
        String user = prop(propsPath, "MYSQL_USERNAME");
        String pass = prop(propsPath, "MYSQL_PASSWORD");
        if (url == null || user == null || pass == null) {
            System.out.println("缺少数据库凭据：请创建 tools/db/db.local.properties（参考 db.local.properties.example）"
                    + "或设置环境变量 MYSQL_URL/MYSQL_USERNAME/MYSQL_PASSWORD");
            System.exit(1);
        }

        String fileName = Path.of(sqlFile).getFileName().toString();
        Integer version = changeVersionOf(fileName);
        String sql = Files.readString(Path.of(sqlFile));

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement st = conn.createStatement()) {
            if (version != null) {
                st.execute(VERSION_TABLE_DDL);
                String appliedAt = appliedAt(st, version);
                if (appliedAt != null) {
                    System.out.println("SKIP " + fileName + "（版本 " + version + " 已于 " + appliedAt + " 应用）");
                    return;
                }
            }
            for (String stmt : sql.split(";\\s*(\\R|$)")) {
                StringBuilder cleaned = new StringBuilder();
                for (String line : stmt.split("\\R")) {
                    String t = line.trim();
                    if (!t.startsWith("--") && !t.isEmpty()) {
                        cleaned.append(line).append('\n');
                    }
                }
                String exec = cleaned.toString().trim();
                if (exec.isEmpty()) {
                    continue;
                }
                String head = exec.substring(0, Math.min(80, exec.length())).replace('\n', ' ');
                try {
                    if (st.execute(exec)) {
                        try (ResultSet rs = st.getResultSet()) {
                            ResultSetMetaData md = rs.getMetaData();
                            StringBuilder sb = new StringBuilder();
                            for (int i = 1; i <= md.getColumnCount(); i++) {
                                sb.append(md.getColumnLabel(i)).append(" | ");
                            }
                            System.out.println(sb);
                            while (rs.next()) {
                                sb = new StringBuilder();
                                for (int i = 1; i <= md.getColumnCount(); i++) {
                                    sb.append(rs.getString(i)).append(" | ");
                                }
                                System.out.println(sb);
                            }
                        }
                    } else {
                        System.out.println("OK (" + st.getUpdateCount() + ") " + head);
                    }
                } catch (SQLException e) {
                    System.out.println("ERROR: " + e.getMessage() + "  <<< " + head);
                    System.exit(2);
                }
            }
            if (version != null) {
                st.execute("INSERT INTO schema_migrations(version, script) VALUES ("
                        + version + ", '" + fileName + "')");
                System.out.println("RECORDED version " + version + " -> schema_migrations");
            }
        }
        System.out.println("DONE");
    }

    /** change 脚本文件名提取版本号；非 change 命名返回 null */
    private static Integer changeVersionOf(String fileName) {
        Matcher m = CHANGE_FILE.matcher(fileName);
        return m.matches() ? Integer.valueOf(m.group(1)) : null;
    }

    /** 查版本账：已应用返回 applied_at 文本，未应用返回 null */
    private static String appliedAt(Statement st, int version) throws SQLException {
        try (ResultSet rs = st.executeQuery(
                "SELECT applied_at FROM schema_migrations WHERE version = " + version)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    /** 配置读取：properties 文件优先，回落环境变量 */
    private static String prop(String propsPath, String key) {
        if (propsPath != null && Files.exists(Path.of(propsPath))) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(Path.of(propsPath))) {
                p.load(in);
                String v = p.getProperty(key);
                if (v != null && !v.isBlank()) {
                    return v.trim();
                }
            } catch (IOException e) {
                System.out.println("读取 properties 失败: " + e.getMessage());
            }
        }
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
