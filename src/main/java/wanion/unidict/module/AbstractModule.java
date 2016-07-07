package wanion.unidict.module;

/*
 * Created by WanionCane(https://github.com/WanionCane).
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 1.1. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/1.1/.
 */

import org.apache.logging.log4j.Logger;
import wanion.unidict.LoadStage;
import wanion.unidict.UniDict;
import wanion.unidict.common.Instantiator;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class AbstractModule
{
    private final String moduleName;

    protected AbstractModule(@Nonnull final String moduleName)
    {
        this.moduleName = moduleName;
    }

    @Nonnull
    protected abstract Manager getAdder();

    protected abstract void init(@Nonnull final Manager manager);

    final void start(@Nonnull final LoadStage loadStage, @Nonnull final Manager manager)
    {
        final List<AbstractModuleThread> threadList = manager.getInstances(loadStage);
        if (threadList.isEmpty())
            return;
        final ExecutorService moduleThreadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final Logger logger = UniDict.getLogger();
        try {
            final long initialTime = System.nanoTime();
            final List<Future<String>> futureOfThreads = moduleThreadExecutor.invokeAll(threadList);
            final long took = System.nanoTime() - initialTime;
            for (Future<String> threadModuleSay : futureOfThreads)
                logger.info(threadModuleSay.get());
            logger.info("All " + threadList.size() + " " + moduleName + "s took " + took / 1000000 + "ms to finish. " + loadStage.name());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Something really bad happened on: " + moduleName);
            e.printStackTrace();
        }
    }

    protected static class Manager
    {
        private final Map<LoadStage, Set<Class<? extends AbstractModuleThread>>> loadStageMap;
        private final Instantiator<AbstractModuleThread> instantiator;

        public Manager(@Nonnull final Instantiator<AbstractModuleThread> instantiator)
        {
            this.instantiator = instantiator;
            loadStageMap = new EnumMap<>(LoadStage.class);
            for (final LoadStage loadStage : LoadStage.values())
                loadStageMap.put(loadStage, new LinkedHashSet<>());
        }

        public boolean add(@Nonnull final Class<? extends AbstractModuleThread> moduleThreadClass)
        {
            final LoadStage loadStage = (moduleThreadClass.isAnnotationPresent(SpecifiedLoadStage.class)) ? moduleThreadClass.getAnnotation(SpecifiedLoadStage.class).stage() : LoadStage.POST_INIT;
            final Set<Class<? extends AbstractModuleThread>> classSet = loadStageMap.get(loadStage);
            return !classSet.contains(moduleThreadClass) && classSet.add(moduleThreadClass);
        }

        boolean isEmpty()
        {
            return loadStageMap.values().stream().allMatch(Set::isEmpty);
        }

        boolean isEmpty(final LoadStage loadStage)
        {
            return loadStageMap.get(loadStage).isEmpty();
        }

        List<AbstractModuleThread> getInstances(final LoadStage loadStage)
        {
            final List<AbstractModuleThread> abstractModuleThreads = new ArrayList<>();
            loadStageMap.get(loadStage).forEach(t -> {
                try {
                    abstractModuleThreads.add(instantiator.instantiate(t));
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            });
            return abstractModuleThreads;
        }
    }
}