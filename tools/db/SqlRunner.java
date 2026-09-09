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

/**
 * SQL 执行器（臻心减本机工具）：按分号切分执行 SQL 文件，结果输出到 stdout
 *
 * 凭据解析顺序：
 *   1. 命令行传入的 .properties 文件路径
 *   2. 环境变量 MYSQL_URL / MYSQL_USERNAME / MYSQL_PASSWORD
 * 推荐：tools\db\run-sql.cmd 会自动传入 db.local.properties（gitignored）
 *
 * 用法: java -cp <mysql-connector.jar>;<编译输出目录> SqlRunner <sql文件> [--allow-error] [xx.properties]
 * 作者: wanglx
 */
public class SqlRunner {

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        String sqlFile = null;
        String propsPath = null;
        boolean allowError = false;
        for (String a : args) {
            if ("--allow-error".equals(a)) {
                allowError = true;
            } else if (a.endsWith(".properties")) {
                propsPath = a;
            } else {
                sqlFile = a;
            }
        }
        if (sqlFile == null) {
            System.out.println("用法: SqlRunner <sql文件> [--allow-error] [db.local.properties]");
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

        String sql = Files.readString(Path.of(sqlFile));
        int errors = 0;
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement st = conn.createStatement()) {
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
                    errors++;
                    System.out.println("ERROR: " + e.getMessage() + "  <<< " + head);
                    if (!allowError) {
                        System.exit(2);
                    }
                }
            }
        }
        System.out.println(errors == 0 ? "DONE" : "DONE_WITH_" + errors + "_ERRORS");
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
