package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.InputStream;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;
import static org.quartz.TriggerBuilder.*;

public class AlertRabbit {

    /**
     * Метод чтения файла с настройками, который будет возвращать загруженный Properties
     */
    private static Properties init() {
        try (InputStream input = AlertRabbit.class.getClassLoader()
                .getResourceAsStream("rabbit.properties")) {
            Properties config = new Properties();
           config.load(input);
           return config;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void main(String[] args) {
        try {
            /*
             * Создание класса, управляющего всеми работами.
             * В объект Scheduler мы будем добавлять задачи, которые хотим выполнять периодически
             */
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            /*
             * Создание задачи.
             * quartz каждый раз создает объект с типом org.quartz.Job.
             * Класс, реализующий этот интерфейс - Rabbit
             */
            JobDetail job = newJob(Rabbit.class).build();

            /*
             * Создание расписания. Настраивает периодичность запуска.
             * В нашем случае запуск задачи происходит через определенное количество секунд.
             * Период обновления хранится в файле rabbit.properties по ключу rabbit.interval.
             * repeatForever() - означает что делаем это бесконечною
             */
            SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInSeconds(Integer.parseInt(
                            init().getProperty("rabbit.interval"))
                    )
                    .repeatForever();

            /*
             * Задача выполняется через триггер.
             * Здесь можно указать, когда начинать запуск.
             * Мы хотим сделать это сразу - startNow()
             */
            Trigger trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();

            /*
             * Загрузка задачи и триггера в планировщик.
             */
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException se) {
            se.printStackTrace();
        }
    }

    /**
     * Внутри этого класса нужно описать требуемые действия.
     * В нашем случае - это вывод на консоль текста
     */
    public static class Rabbit implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
        }
    }
}