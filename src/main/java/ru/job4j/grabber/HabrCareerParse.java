package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.List;

public class HabrCareerParse {
    private static final String SOURCE_LINK = "https://career.habr.com";
    public static final String PREFIX = "/vacancies?page=";
    public static final String SUFFIX = "&q=Java%20developer&type=all";

    /**
     * Метод для загрузки деталей объявления.
     * В методе используем eachText(), который собирает в коллекцию текстовое содержание
     * каждого элемента.
     * Затем с помощью StringBuilder формируем результирующую строку из полученных элементов коллекции.
     * @param link - ссылка на вакансию.
     * @return - строка с распаршенным описанием вакансии.
     */
    private String retrieveDescription(String link) throws IOException {
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        List<String> description = document.select(".vacancy-description__text").eachText();
        StringBuilder builder = new StringBuilder();
        description.forEach(builder::append);
        return builder.toString();
    }

    public static void main(String[] args) throws IOException {
        int pageNumber = 5;
        for (int i = 1; i <= pageNumber;  i++) {
            String fullLink = "%s%s%d%s".formatted(SOURCE_LINK, PREFIX, i, SUFFIX);
            Connection connection = Jsoup.connect(fullLink);
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                Element dateElement = row.select(".vacancy-card__date").first();
                String vacancyName = titleElement.text();
                String vacancyDate = dateElement.child(0).attr("datetime");
                String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                System.out.printf("%s %s %s%n", vacancyName, vacancyDate, link);
            });
        }
    }
}