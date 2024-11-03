package view;

import model.Book;
import model.Genre;
import model.Reader;
import model.Role;
import service.LibraryService;

import utils.MyArrayList;
import utils.MyList;

import java.io.InputStream;
import java.util.Properties;

import java.util.Scanner;

import static view.Color.*;

/**
 * Group: 52-1, "AIT Hi-tech team" GMBH
 * Author: Bogdan Fesenko
 * Date: 02-11-2024
 */
/*

 */
public class LibraryView {

    private final LibraryService libraryService;
    private Reader currentReader;

    public LibraryView(LibraryService libraryService) {
        this.libraryService = libraryService;
        this.currentReader = null;
    }

    // проверяем список книг
    private void showAllBooks() {
        MyList<Book> books = libraryService.getAllBooks();
        if (books == null || books.isEmpty()) {  // проверка на null или пустой список
            System.out.println("Список книг пуст.");
            return;
        }
        System.out.println(COLOR_YELLOW + "Список всех книг:" + COLOR_RESET);
        System.out.println(printBooks(books));
    }

    private String loadApiKey() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("openai.properties")) {
            if (input == null) {
                System.out.println("API ключ не найден. Пожалуйста, проверьте файл openai.properties.");
                return null;
            }
            properties.load(input);
            return properties.getProperty("OPENAI_API_KEY");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }


    private String printBooks(MyList<Book> books) {
        // Определяем максимальные ширины для каждого столбца без учета цветовых кодов
        int maxIdWidth = 5;
        int maxTitleWidth = "Название".length();
        int maxAuthorWidth = "Автор".length();
        int maxYearWidth = 5;
        int maxPagesWidth = 8;
        int maxLanguageWidth = "Язык".length();
        int maxStatusWidth = 10;
        int maxGenreWidth = "Жанр".length();

        // Вычисляем максимальную длину текста в каждом столбце (без учета цвета)
        for (Book book : books) {
            maxTitleWidth = Math.max(maxTitleWidth, book.getTitle().length());
            maxAuthorWidth = Math.max(maxAuthorWidth, book.getAuthor().length());
            maxPagesWidth = Math.max(maxPagesWidth, String.valueOf(book.getPageCount()).length());
            maxLanguageWidth = Math.max(maxLanguageWidth, book.getLanguage().length());
            maxGenreWidth = Math.max(maxGenreWidth, book.getDescriptions().replaceAll("\u001B\\[[;\\d]*m", "").length());
        }

        // Формат строки с динамическими размерами колонок, "Жанр" и "Язык" перемещены в конец
        String format = String.format("%%-%ds %%-%ds %%-%ds %%-%ds %%-%ds %%-%ds %%-%ds %%-%ds\n",
                maxIdWidth, maxTitleWidth, maxAuthorWidth, maxYearWidth, maxPagesWidth, maxLanguageWidth, maxStatusWidth, maxGenreWidth);

        // Заголовок таблицы с динамическими размерами колонок
        StringBuilder result = new StringBuilder(String.format("\u001B[33m" + format + COLOR_RESET,
                "ID", "Название", "Автор", "Год", "Страницы", "Язык", "Статус", "Жанр"));

        // Строка-разделитель
        int totalWidth = maxIdWidth + maxTitleWidth + maxAuthorWidth + maxYearWidth + maxPagesWidth + maxLanguageWidth + maxStatusWidth + maxGenreWidth + 7; // 7 пробелов между колонками
        result.append("=".repeat(totalWidth) + "\n");

        // Форматируем строки для каждой книги
        for (Book book : books) {
            // Определяем доступность книги как "доступна" или "занята" с цветом
            String availability = book.isAvailable() ? COLOR_GREEN + "доступна" + COLOR_RESET : COLOR_RED + "занята" + COLOR_RESET;

            // Определяем цвет жанра и форматируем его для вывода
            String[] genres = book.getDescriptions().split(",");
            StringBuilder coloredGenres = new StringBuilder();
            for (String genreStr : genres) {
                Genre genreEnum = Genre.getGenre(genreStr.trim());
                if (genreEnum != null) {
                    // Добавляем жанр с его цветом
                    coloredGenres.append(genreEnum.getColor()).append(genreEnum.getDescription()).append(COLOR_RESET).append(", ");
                } else {
                    // Если жанр не найден, выводим без цвета
                    coloredGenres.append(genreStr.trim()).append(", ");
                }
            }
            // Удаляем последнюю запятую и пробел
            if (coloredGenres.length() > 0) {
                coloredGenres.setLength(coloredGenres.length() - 2);
            }

            // Добавляем данные книги с использованием динамического формата, "Язык" после "Страницы"
            result.append(String.format(format,
                    book.getId(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.getYear(),
                    book.getPageCount(),
                    book.getLanguage(),
                    availability,
                    coloredGenres.toString()));
        }

        return result.toString();
    }




    // проверяем доступность книги даже при частичном вводе названия
    private void searchBookByTitle(Scanner scanner) {
        System.out.print("Введите название книги или его часть: ");
        String title = scanner.nextLine();
        MyList<Book> books = libraryService.searchBooksByTitle(title);
        if (!books.isEmpty()) {
            System.out.println(COLOR_YELLOW + "Найденные книги:" + COLOR_RESET);
            System.out.println(printBooks(books));
        } else {
            System.out.println("Книги не найдены.");
        }
    }

    // находим книгу по автору или частичному его название
    private void searchBookByAuthor(Scanner scanner) {
        System.out.print("Введите имя автора или его часть: ");
        String author = scanner.nextLine();
        MyList<Book> books = libraryService.searchBooksByAuthor(author);
        if (!books.isEmpty()) {
            System.out.println(COLOR_YELLOW + "Найденные книги:" + COLOR_RESET);
            System.out.println(printBooks(books));
        } else {
            System.out.println("Книги не найдены.");
        }
    }

    // вот пользователям книги которую хочет взять
    private void borrowBook(Scanner scanner) {
        System.out.print("Введите название книги, которую хотите взять: ");
        String title = scanner.nextLine();
        boolean success = libraryService.borrowBook(title, currentReader.getName());
        if (success) {
            System.out.println(COLOR_GREEN + "Книга успешно взята." + COLOR_RESET);
        } else {
            System.out.println(COLOR_RED + "Не удалось взять книгу. Возможно, она занята или не существует." + COLOR_RESET);
        }
    }

    // вот пользователям название книги которую хочет вернуть
    private void returnBook(Scanner scanner) {
        System.out.print("Введите название книги, которую хотите вернуть: ");
        String title = scanner.nextLine();
        boolean success = libraryService.returnBook(title, currentReader.getName());
        if (success) {
            System.out.println(COLOR_GREEN + "Книга успешно возвращена." + COLOR_RESET);
        } else {
            System.out.println(COLOR_RED + "Не удалось вернуть книгу. Возможно, она не числится за вами." + COLOR_RESET);
        }
    }

    // выводим список доступных книг
    private void showAvailableBooks() {
        MyList<Book> books = libraryService.getAllAvailableBooks();
        System.out.println(COLOR_YELLOW + "Список доступных книг:" + COLOR_RESET);
        System.out.println(printBooks(books));
    }

    // выводим список занятых книг
    private void showBorrowedBooks() {
        MyList<Book> books = libraryService.getAllBorrowedBooks();
        System.out.println(COLOR_YELLOW + "Список занятых книг:" + COLOR_RESET);
        System.out.println(printBooks(books));
    }

    // будет список книг которую взял пользователь
    private void showMyBooks() {
        MyList<Book> books = libraryService.getBooksBorrowedByReader(currentReader.getName());
        if (books != null && !books.isEmpty()) {
            System.out.println(COLOR_YELLOW + "Книги, которые вы взяли:" + COLOR_RESET);
            System.out.println(printBooks(books));
        } else {
            System.out.println("У вас нет взятых книг.");
        }
    }

    // добавляем книгу
    private void addBook(Scanner scanner) {
        System.out.print("Введите название книги: ");
        String title = scanner.nextLine();
        System.out.print("Введите автора книги: ");
        String author = scanner.nextLine();
        System.out.print("Введите год книги: ");
        int year = scanner.nextInt();
        System.out.print("Введите жанр книги: ");
        String genre = scanner.nextLine();
        MyList<Genre> genres = new MyArrayList<>();
        while (!genre.equals("0")) {
            Genre newGenre = Genre.getGenre(genre);
            if (newGenre != null) {
                genres.add(newGenre);
            }
            System.out.print("Введите жанр книги или 0 для выхода: ");
            genre = scanner.nextLine();
        }
        libraryService.addBook(title, author, year, genres);
        System.out.println(COLOR_GREEN + "Книга успешно добавлена." + COLOR_RESET);
    }

    // редактировать книгу
    private void editBook(Scanner scanner) {
        System.out.print("Введите название книги, которую хотите отредактировать: ");
        String oldTitle = scanner.nextLine();
        System.out.print("Введите новое название книги: ");
        String newTitle = scanner.nextLine();
        System.out.print("Введите нового автора книги: ");
        String newAuthor = scanner.nextLine();
        boolean success = libraryService.editBook(oldTitle, newTitle, newAuthor, currentReader);
        if (success) {
            System.out.println(COLOR_GREEN + "Книга успешно отредактирована." + COLOR_RESET);
        } else {
            System.out.println(COLOR_RED + "Не удалось отредактировать книгу. Возможно, книга не найдена." + COLOR_RESET);
        }
    }

    // выводим на печать кто взял книгу название книги
    private void viewBookBorrower(Scanner scanner) {
        System.out.print("Введите название книги: ");
        String title = scanner.nextLine();
        // Get all readers
        MyList<Reader> readers = libraryService.getReaderRepository().getAllReaders();

        for (Reader reader : readers) {
            MyList<Book> borrowedBooks = reader.getBorrowedBooks();
            MyList<Book> books = new MyArrayList<>();

            for (Book book : borrowedBooks) {
                if (book.getTitle().toLowerCase().contains(title.toLowerCase())) {
                    books.add(book);
                }
            }
            if (!books.isEmpty()) {
                System.out.println(reader.getName() + "  взял:");
                System.out.println(printBooks(books));
            }
        }
    }




    public void run() {
        // сканируем ввод пользователя
        Scanner scanner = new Scanner(System.in);



        while (true) {
            if (currentReader == null) {
                System.out.print("Добро пожаловать в библиотеку ");
                System.out.println(COLOR_BLUE + "\"Знания Века\"" + COLOR_RESET + "!");
                System.out.print("Пожалуйста, введите ваше имя для авторизации, используя латинские буквы: ");

                String name = scanner.nextLine();
                System.out.print("Введите ваш password: ");
                String password = scanner.nextLine();

                String authenticated = libraryService
                        .getSecurity()
                        .authenticateUser(libraryService.getReaderByName(name), password);
                System.out.println(authenticated);
                if (authenticated.equals("Аутентификация успешна!")) {
                    currentReader = libraryService.authenticateReader(name);
                }

                if (currentReader == null) {
                    System.out.print("Пользователь не найден. Желаете зарегистрироваться? (да/нет, yes/no): ");
                    String answer = scanner.nextLine();
                    if (answer.equalsIgnoreCase("да") || answer.equalsIgnoreCase("yes")) {
                        System.out.print("Введите ваш email: ");
                        String email = scanner.nextLine();
                        while (!libraryService.getValidator().isValidEmail(email)) {
                            System.out.println("\"Ваш email " +
                                    "должен содержать латинские буквы, цифры, символ '@' и домен, например, " +
                                    "'name@example.com'.\" ");
                            System.out.print("Введите ваш email: ");
                            email = scanner.nextLine();
                        }
                        System.out.print("Введите ваш password: ");
                        password = scanner.nextLine();
                        while (!libraryService.getValidator().isValidPassword(password)) {
                            System.out.println("Ваш пароль должен содержать минимум 8 символов, хотя бы одна заглавная и " +
                                    "строчная буква, хотя бы одна цифра и один спецсимвол, например: @, #, $.");
                            System.out.print("Введите ваш password: ");
                            password = scanner.nextLine();
                        }
                        libraryService.registerReader(name, email, password, "READER");
                        currentReader = libraryService.authenticateReader(name);
                        System.out.println("Регистрация успешна!");
                    } else {
                        continue;
                    }
                }
            }

            System.out.println(COLOR_CYAN + "\n===== Меню =====" + COLOR_RESET);
            System.out.println("1. Показать все книги");
            System.out.println("2. Поиск книги по названию");
            System.out.println("3. Поиск книги по автору");
            System.out.println("4. Взять книгу");
            System.out.println("5. Вернуть книгу");
            System.out.println("6. Показать доступные книги");
            System.out.println("7. Показать занятые книги");
            System.out.println("8. Показать мои книги");
            System.out.println("9. Сортировать книги по названию");
            System.out.println("10. Сортировать книги по автору");
            if (currentReader.getRole() == Role.ADMIN) {
                System.out.println("11. Добавить книгу");
                System.out.println("12. Редактировать книгу");
                System.out.println("13. Посмотреть, у кого находится книга");
            }
            System.out.println("0. Выйти");
            System.out.print("Выберите действие: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    showAllBooks();
                    break;
                case "2":
                    searchBookByTitle(scanner);
                    break;
                case "3":
                    searchBookByAuthor(scanner);
                    break;
                case "4":
                    borrowBook(scanner);
                    break;
                case "5":
                    returnBook(scanner);
                    break;
                case "6":
                    showAvailableBooks();
                    break;
                case "7":
                    showBorrowedBooks();
                    break;
                case "8":
                    showMyBooks();
                    break;
                case "9":
                    libraryService.sortBooksByTitle();
                    System.out.println("Книги отсортированы по названию.");
                    break;
                case "10":
                    libraryService.sortBooksByAuthor();
                    System.out.println("Книги отсортированы по автору.");
                    break;
                case "11":
                    if (currentReader.getRole() == Role.ADMIN) {
                        addBook(scanner);
                    } else {
                        System.out.println("Неверный выбор.");
                    }
                    break;
                case "12":
                    if (currentReader.getRole() == Role.ADMIN) {
                        editBook(scanner);
                    } else {
                        System.out.println("Неверный выбор.");
                    }
                    break;
                case "13":
                    if (currentReader.getRole() == Role.ADMIN) {
                        viewBookBorrower(scanner);
                    } else {
                        System.out.println("Неверный выбор.");
                    }
                    break;




                case "0":
                    currentReader = null;
                    System.out.println("Вы вышли из системы.");
                    break;
                default:
                    System.out.println("Неверный выбор.");
            }
        }
    }
}
