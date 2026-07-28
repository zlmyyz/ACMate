package com.itnoduck.acmate.testutil;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.training.entity.TrainingPlan;
import com.itnoduck.acmate.training.entity.TrainingPlanMember;
import com.itnoduck.acmate.training.entity.TrainingPlanProblem;
import com.itnoduck.acmate.training.mapper.TrainingPlanMapper;
import com.itnoduck.acmate.training.mapper.TrainingPlanMemberMapper;
import com.itnoduck.acmate.training.mapper.TrainingPlanProblemMapper;
import com.itnoduck.acmate.notification.entity.Notification;
import com.itnoduck.acmate.notification.mapper.NotificationMapper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;

/**
 * Initializes MyBatis-Plus entity lambda caches for unit tests
 * that run without a Spring context (pure Mockito / MockitoExtension).
 */
public final class MybatisPlusTestHelper {

    private static volatile boolean initialized;

    private MybatisPlusTestHelper() {}

    public static synchronized void initEntityTables() {
        if (initialized) {
            return;
        }
        MybatisConfiguration config = new MybatisConfiguration();
        config.addMapper(AppUserMapper.class);
        config.addMapper(ProblemMapper.class);
        config.addMapper(TrainingPlanMapper.class);
        config.addMapper(TrainingPlanProblemMapper.class);
        config.addMapper(TrainingPlanMemberMapper.class);
        config.addMapper(NotificationMapper.class);

        if (TableInfoHelper.getTableInfo(TrainingPlan.class) == null
                || TableInfoHelper.getTableInfo(TrainingPlanProblem.class) == null
                || TableInfoHelper.getTableInfo(TrainingPlanMember.class) == null) {
            throw new IllegalStateException(
                    "Failed to initialize MyBatis-Plus TableInfo for training entities.");
        }
        initialized = true;
    }
}
