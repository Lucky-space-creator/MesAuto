package com.lucky.mescore.common.config;

import com.lucky.mescore.common.page.PageRequest;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

/**
 * MyBatis-Plus 在该内网仓库中的分页插件（PaginationInnerInterceptor）被裁剪，
 * 因此这里自行实现一个基于 MyBatis 原生 Interceptor 的 MySQL 物理分页插件，
 * 对参数中包含 {@link PageRequest} 的查询方法自动进行 count + limit 分页。
 */
@Configuration
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class MyBatisPlusConfig implements Interceptor {

    private static final String COUNT_SUFFIX = "_COUNT";

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        // 仅对 SELECT 且携带 PageRequest 参数的查询进行分页
        if (ms.getSqlCommandType() != SqlCommandType.SELECT || !(parameter instanceof PageRequest)) {
            return invocation.proceed();
        }

        PageRequest<?> pageRequest = (PageRequest<?>) parameter;
        long pageNum = Math.max(pageRequest.getPageNum(), 1);
        long pageSize = Math.max(pageRequest.getPageSize(), 1);

        Executor executor = (Executor) invocation.getTarget();
        BoundSql boundSql = ms.getBoundSql(parameter);
        String sql = boundSql.getSql();

        // 1. 执行 count 查询，将总数回写到 PageRequest
        long total = executeCount(executor, ms, boundSql, parameter, sql);
        pageRequest.setTotal(total);

        // 2. 改写原 SQL 追加 LIMIT 分页
        String pageSql = sql + " LIMIT " + (pageNum - 1) * pageSize + ", " + pageSize;
        BoundSql pageBoundSql = new BoundSql(ms.getConfiguration(), pageSql,
                boundSql.getParameterMappings(), boundSql.getParameterObject());

        MappedStatement pageMs = copyMappedStatement(ms, pageBoundSql);
        args[0] = pageMs;
        args[1] = parameter;
        args[2] = RowBounds.DEFAULT;

        return invocation.proceed();
    }

    private long executeCount(Executor executor, MappedStatement ms, BoundSql boundSql,
                              Object parameter, String sql) throws Exception {
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") tmp_count";
        BoundSql countBoundSql = new BoundSql(ms.getConfiguration(), countSql,
                boundSql.getParameterMappings(), boundSql.getParameterObject());
        MappedStatement countMs = copyMappedStatement(ms, countBoundSql, COUNT_SUFFIX);

        Connection connection = executor.getTransaction().getConnection();
        try (PreparedStatement ps = connection.prepareStatement(countSql)) {
            new DefaultParameterHandler(ms, boundSql.getParameterObject(), countBoundSql).setParameters(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    private MappedStatement copyMappedStatement(MappedStatement ms, BoundSql newBoundSql) {
        return copyMappedStatement(ms, newBoundSql, "_PAGE");
    }

    private MappedStatement copyMappedStatement(MappedStatement ms, BoundSql newBoundSql, String suffix) {
        MappedStatement.Builder builder = new MappedStatement.Builder(
                ms.getConfiguration(), ms.getId() + suffix,
                new BoundSqlSqlSource(newBoundSql), ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        return builder.build();
    }

    @Override
    public Object plugin(Object target) {
        return Interceptor.super.plugin(target);
    }

    @Override
    public void setProperties(Properties properties) {
        // no-op
    }

    /**
     * 用一个已有的 BoundSql 构造 SqlSource，便于复用参数映射。
     */
    private static class BoundSqlSqlSource implements org.apache.ibatis.mapping.SqlSource {
        private final BoundSql boundSql;

        BoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }
}
