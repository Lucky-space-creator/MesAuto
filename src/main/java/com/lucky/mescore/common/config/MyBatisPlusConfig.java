package com.lucky.mescore.common.config;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

/**
 * MyBatis-Plus 在该内网仓库中的分页插件（PaginationInnerInterceptor）被裁剪，
 * 因此这里自行实现一个基于 MyBatis 原生 Interceptor 的 MySQL 物理分页插件，
 * 对参数中包含 {@link PageRequest} 的查询方法自动进行 count + limit 分页。
 *
 * 注意：必须用 @Component 而非 @Configuration。@Configuration 类会被 Spring
 * 用 CGLIB 代理，导致 MyBatis 在注册插件时反射读取 @Intercepts 注解失败
 * （CGLIB 子类不继承注解），报 No @Intercepts annotation was found。
 */
@Component
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

        if (ms.getSqlCommandType() != SqlCommandType.SELECT) {
            return invocation.proceed();
        }

        // 支持两种分页入参：自定义 PageRequest，以及 MyBatis-Plus 的 IPage
        PageRequest<?> pageRequest = findPageRequest(parameter);
        IPage<?> mpPage = pageRequest == null ? findPage(parameter) : null;
        if (pageRequest == null && mpPage == null) {
            return invocation.proceed();
        }

        long pageNum;
        long pageSize;
        if (pageRequest != null) {
            pageNum = Math.max(pageRequest.getPageNum(), 1);
            pageSize = Math.max(pageRequest.getPageSize(), 1);
        } else {
            pageNum = Math.max(mpPage.getCurrent(), 1);
            pageSize = Math.max(mpPage.getSize(), 1);
        }

        Executor executor = (Executor) invocation.getTarget();
        BoundSql boundSql = ms.getBoundSql(parameter);
        String sql = boundSql.getSql();

        // 1. 执行 count 查询，将总数回写到分页对象
        long total = executeCount(executor, ms, boundSql, parameter, sql);
        if (pageRequest != null) {
            pageRequest.setTotal(total);
        } else {
            mpPage.setTotal(total);
            mpPage.setCurrent(pageNum);
            mpPage.setSize(pageSize);
        }

        // 2. 改写原 SQL 追加 LIMIT 分页
        String pageSql = sql + " LIMIT " + (pageNum - 1) * pageSize + ", " + pageSize;
        BoundSql pageBoundSql = new BoundSql(ms.getConfiguration(), pageSql,
                boundSql.getParameterMappings(), boundSql.getParameterObject());

        MappedStatement pageMs = copyMappedStatement(ms, pageBoundSql);
        args[0] = pageMs;
        args[1] = parameter;
        args[2] = RowBounds.DEFAULT;

        Object result = invocation.proceed();

        // MyBatis-Plus 的 service.page() 直接从 IPage 取 records，需回填
        if (mpPage != null && result instanceof java.util.List) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            IPage rawPage = mpPage;
            rawPage.setRecords((java.util.List) result);
        }
        return result;
    }

    /** 从 MyBatis 参数（可能是 ParamMap）中查找 PageRequest */
    private PageRequest<?> findPageRequest(Object parameter) {
        if (parameter instanceof PageRequest) {
            return (PageRequest<?>) parameter;
        }
        if (parameter instanceof java.util.Map) {
            for (Object v : ((java.util.Map<?, ?>) parameter).values()) {
                if (v instanceof PageRequest) {
                    return (PageRequest<?>) v;
                }
            }
        }
        return null;
    }

    /** 从 MyBatis 参数（可能是 ParamMap）中查找 MyBatis-Plus 的 IPage */
    private IPage<?> findPage(Object parameter) {
        if (parameter instanceof IPage) {
            return (IPage<?>) parameter;
        }
        if (parameter instanceof java.util.Map) {
            for (Object v : ((java.util.Map<?, ?>) parameter).values()) {
                if (v instanceof IPage) {
                    return (IPage<?>) v;
                }
            }
        }
        return null;
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
        return org.apache.ibatis.plugin.Plugin.wrap(target, this);
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
