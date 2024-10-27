package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;
import static org.quartz.TriggerBuilder.*;

public class AlertRabbit {

    /*
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

    /*
     * scheduler - Создание класса, управляющего всеми работами.
     * В объект Scheduler мы будем добавлять задачи, которые хотим выполнять периодически
     * job - Создание задачи. Quartz каждый раз создает объект с типом org.quartz.Job.
     * Класс, реализующий этот интерфейс - Rabbit
     * times - Создание расписания. Настраивает периодичность запуска. В нашем случае запуск задачи происходит
     * через определенное количество секунд. Период обновления хранится в файле rabbit.properties
     * по ключу rabbit.interval. repeatForever() - означает что делаем это бесконечно.
     * trigger - Задача выполняется через триггер. Здесь можно указать, когда начинать запуск.
     * Мы хотим сделать это сразу - startNow()
     * scheduler.scheduleJob(job, trigger) - Загрузка задачи и триггера в планировщик.
     * Весь main работает 10 секунд
     */

    public static void main(String[] args) throws ClassNotFoundException {
        Class.forName(init().getProperty("jdbc.driver-class-name"));
        try {
            Connection con = DriverManager.getConnection(
                   init().getProperty("jdbc.url"),
                   init().getProperty("jdbc.username"),
                   init().getProperty("jdbc.password")
            );
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDataMap data = new JobDataMap();
            data.put("connection", con);
            JobDetail job = newJob(Rabbit.class)
                    .usingJobData(data)
                    .build();
            SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInSeconds(Integer.parseInt(
                            init().getProperty("rabbit.interval"))
                    )
                    .repeatForever();
            Trigger trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
            Thread.sleep(10000);
            scheduler.shutdown();
            con.close();
        } catch (Exception se) {
            se.printStackTrace();
        }
    }

    /*
     * Внутри этого класса нужно описать требуемые действия.
     */
    public static class Rabbit implements Job {

        public Rabbit() {
            System.out.println(hashCode());
        }

        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
            Connection connection = (Connection) context.getJobDetail().getJobDataMap().get("connection");
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO sql_schema.rabbit(created_date) values (?)")) {
                preparedStatement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                preparedStatement.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}