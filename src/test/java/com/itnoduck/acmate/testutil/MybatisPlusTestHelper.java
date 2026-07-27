package com.itnoduck.acmate.testutil;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;

/**
 * Initializes MyBatis-Plus entity lambda caches for unit tests
 * that run without a Spring context (pure Mockito / MockitoExtension).
 *
 * <p>Without Spring, MyBatis-Plus auto-configuration never runs,
 * so {@code LambdaQueryWrapper} and {@code LambdaUpdateWrapper}
 * can't resolve entity property-to-column mappings.
 */
public final class MybatisPlusTestHelper {

    private static volatile boolean initialized;

    private MybatisPlusTestHelper() {}

    /**
     * Call once per test class — safe to call multiple times (idempotent).
     */
    public static synchronized void initEntityTables() {
        if (initialized) {
            return;
        }
        MybatisConfiguration config = new MybatisConfiguration();
        // Adding a mapper triggers MybatisMapperAnnotationBuilder.parse(),
        // which calls TableInfoHelper.initTableInfo() for the entity type.
        config.addMapper(AppUserMapper.class);
        config.addMapper(ProblemMapper.class);

        // Verify initialization succeeded
        if (TableInfoHelper.getTableInfo(Problem.class) == null) {
            throw new IllegalStateException(
                    "Failed to initialize MyBatis-Plus TableInfo for Problem. "
                            + "Ensure Problem entity has @TableName and @TableId annotations.");
        }
        initialized = true;
    }
}
