/*
 * Copyright 2026 Boris Tarelkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.max.botapi.spring;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.util.ClassUtils;

import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.core.UpdateHandler;
import ru.max.botapi.model.Nullable;

/**
 * Auto-configuration that validates the {@code max.bot.mode} property.
 *
 * <p>Logs a warning at startup if no mode is configured, guiding the user
 * to set {@code max.bot.mode} to {@code webhook}, {@code longpolling},
 * or {@code none}.</p>
 *
 * <p>Once all beans are created it also warns when a mode is selected but cannot work: no
 * {@link MaxBotAPI} bean (the token property is missing), no {@link UpdateHandler} bean, or,
 * for {@code webhook}, no Spring MVC on the classpath. Otherwise such an application starts
 * cleanly and simply never receives updates.</p>
 *
 * @see MaxBotMode
 * @see MaxBotProperties
 */
@AutoConfiguration
@EnableConfigurationProperties(MaxBotProperties.class)
public class MaxBotModeAutoConfiguration
        implements InitializingBean, SmartInitializingSingleton, BeanFactoryAware {

    private static final Logger LOG =
            LoggerFactory.getLogger(MaxBotModeAutoConfiguration.class);

    private static final String DISPATCHER_SERVLET =
            "org.springframework.web.servlet.DispatcherServlet";

    private final MaxBotProperties properties;
    private @Nullable ListableBeanFactory beanFactory;

    /**
     * Creates the auto-configuration with the bound properties.
     *
     * @param properties the root MAX Bot properties
     */
    public MaxBotModeAutoConfiguration(MaxBotProperties properties) {
        this.properties = properties;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory instanceof ListableBeanFactory listable ? listable : null;
    }

    /**
     * Checks whether {@code max.bot.mode} is set and logs a warning
     * if it is not.
     */
    @Override
    public void afterPropertiesSet() {
        if (properties.getMode() == null) {
            LOG.warn("No max.bot.mode configured. Set max.bot.mode "
                    + "to 'webhook', 'longpolling', or 'none'.");
        } else if (properties.getMode() == MaxBotMode.NONE) {
            LOG.info("max.bot.mode is set to 'none' "
                    + "— bot update delivery is disabled.");
        }
    }

    /**
     * Warns when the selected mode is missing something it needs to deliver updates.
     */
    @Override
    public void afterSingletonsInstantiated() {
        MaxBotMode mode = properties.getMode();
        if (mode == null || mode == MaxBotMode.NONE || beanFactory == null) {
            return;
        }
        String modeName = mode.name().toLowerCase(Locale.ROOT);
        if (mode == MaxBotMode.WEBHOOK
                && !ClassUtils.isPresent(DISPATCHER_SERVLET, getClass().getClassLoader())) {
            LOG.warn("max.bot.mode=webhook, but Spring MVC is not on the classpath: the webhook "
                    + "endpoint is not registered. Add spring-boot-starter-webmvc.");
            return;
        }
        if (!hasBean(MaxBotAPI.class)) {
            LOG.warn("max.bot.mode={}, but there is no MaxBotAPI bean: updates will not be "
                    + "received. Set max.bot.{}.token or declare a MaxBotAPI bean.",
                    modeName, modeName);
        }
        if (!hasBean(UpdateHandler.class)) {
            LOG.warn("max.bot.mode={}, but there is no UpdateHandler bean: updates will not be "
                    + "received. Declare a bean implementing UpdateHandler.", modeName);
        }
    }

    private boolean hasBean(Class<?> type) {
        return beanFactory != null
                && beanFactory.getBeanNamesForType(type, true, false).length > 0;
    }
}
