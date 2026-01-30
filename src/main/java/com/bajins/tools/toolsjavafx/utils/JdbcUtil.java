package com.bajins.tools.toolsjavafx.utils;

import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.ds.simple.SimpleDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

/**
 * JDBC工具类
 * @author bajin
 */
public class JdbcUtil {
    private static String key = "";
    private static DataSource dataSource;

    /**
     * 创建数据源
     *
     * @param dbType   数据库类型
     * @param ip       数据库IP
     * @param port     数据库端口
     * @param dbName   数据库名称
     * @param user     数据库用户名
     * @param password 数据库密码
     */
    public static void createDataSource(String dbType, String ip, String port, String dbName, String user, String password) {
        String newKey = dbType + ip + port + dbName + user + password;
        if (newKey.equals(key)) {
            return;
        }
        key = newKey;

        // 3.1 动态构建 URL
        String url = buildUrl(dbType, ip, port, dbName);

        // 自定义数据库Setting，更多实用请参阅Hutool-Setting章节
        /*Setting setting = new Setting();
        // 获取指定配置，第二个参数为分组，用于多数据源，无分组情况下传null
        // 注意此处DSFactory需要复用或者关闭
        DSFactory dsFactory = DSFactory.create(setting);
        dataSource = dsFactory.getDataSource();*/

        // SimpleDataSource只是DriverManager.getConnection的简单包装，本身并不支持池化功能，此类特别适合少量数据库连接的操作。
        dataSource = new SimpleDataSource(url, user, password);
    }

    /**
     * 构建数据库连接URL
     *
     * @param type 数据库类型
     * @param ip   数据库IP
     * @param port 数据库端口
     * @param db   数据库名称
     * @return 数据库连接URL
     */
    private static String buildUrl(String type, String ip, String port, String db) {
        if ("MySQL".equals(type)) {
            return String.format("jdbc:mysql://%s:%s/%s?useSSL=false", ip, port, db);
        }
        if ("PostgreSQL".equals(type)) {
            return String.format("jdbc:postgresql://%s:%s/%s", ip, port, db);
        }
        if ("Oracle".equals(type)) {
            return String.format("jdbc:oracle:thin:@%s:%s:%s", ip, port, db);
        }
        if ("SQLServer".equals(type)) {
            return String.format("jdbc:sqlserver://%s:%s;databaseName=%s", ip, port, db);
        }
        return "";
    }

    /**
     * 测试数据库连接是否有效
     *
     * @throws SQLException SQL异常
     */
    public static void testConn() throws SQLException {
        // 参数是超时时间（秒），如果 3 秒内没响应则返回 false
        boolean isValid = dataSource.getConnection().isValid(3);
        if (!isValid) {
            throw new SQLException("连接无效，可能是数据库未启动或配置错误");
        }
    }

    /**
     * 执行查询SQL语句
     *
     * @param sql    SQL语句
     * @param params SQL参数
     * @return 查询结果实体列表
     * @throws SQLException SQL异常
     */
    public static List<Entity> query(String sql, Object... params) throws SQLException {
        return Db.use(dataSource).query(sql, params);
    }
}
