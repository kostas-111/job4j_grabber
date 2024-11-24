package ru.job4j.grabber;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.io.IOException;

public interface Grab {
    void init(Parse parse, Store store, Scheduler scheduler) throws SchedulerException, IOException;
}