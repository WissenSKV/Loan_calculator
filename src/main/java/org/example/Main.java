package org.example;
import java.text.DecimalFormat;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/Customer?user=root&password=root");
            Scanner scanner = new Scanner(System.in);

            boolean exit = false;

            while (!exit) {
                System.out.println("Выберите действие:");
                System.out.println("1) Добавить кредит");
                System.out.println("2) Калькулятор");
                System.out.println("3) Отчет");
                System.out.println("4) Выйти");

                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        addCredit(connection);
                        break;
                    case 2:
                        calculateLoan(scanner);
                        break;
                    case 3:
                        displayAnalytics(connection);
                        break;
                    case 4:
                        exit = true;
                        System.out.println("Программа завершена.");
                        break;
                    default:
                        System.out.println("Некорректный выбор");
                }
            }

            connection.close();
            scanner.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addCredit(Connection connection) {
        try {
            Scanner scanner = new Scanner(System.in);

            System.out.println("Введите данные кредита:");
            System.out.println();
            System.out.print("Имя: ");
            String fName = scanner.nextLine();

            System.out.print("Фамилия: ");
            String lName = scanner.nextLine();

            System.out.print("Сумма кредита: ");
            double creditAmount = scanner.nextDouble();
            scanner.nextLine();

            System.out.print("Процент годовой: ");
            double annualInterestRate = scanner.nextDouble();
            scanner.nextLine();

            System.out.print("Срок кредита (в годах): ");
            int loanTerm = scanner.nextInt();
            scanner.nextLine();

            System.out.print("Дата взятия кредита: ");
            String loanDate = scanner.nextLine();

            insertCreditData(connection, fName, lName, creditAmount, annualInterestRate, loanTerm, loanDate);

            System.out.println("Данные успешно добавлены в базу данных.");
            System.out.println();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void insertCreditData(Connection connection, String fName, String lName, double creditAmount,
                                         double annualInterestRate, int loanTerm, String loanDate) throws SQLException {
        String insertCreditQuery = "INSERT INTO Credit (Client, loan_amount, Annual_interest_rate, Loan_term_years, Loan_Date, Repayment_date, Revenue) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement creditStatement = connection.prepareStatement(insertCreditQuery)) {
            double revenue = calculateRevenue(creditAmount, annualInterestRate, loanTerm);
            creditStatement.setString(1, fName + " " + lName);
            creditStatement.setDouble(2, creditAmount);
            creditStatement.setDouble(3, annualInterestRate);
            creditStatement.setInt(4, loanTerm);
            creditStatement.setString(5, loanDate);
            creditStatement.setDate(6, java.sql.Date.valueOf(calculateRepaymentDate(loanDate, loanTerm)));
            creditStatement.setDouble(7, revenue);

            creditStatement.executeUpdate();
        }
    }

    private static String calculateRepaymentDate(String loanDate, int loanTerm) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = dateFormat.parse(loanDate);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.YEAR, loanTerm);

            return dateFormat.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static double calculateRevenue(double creditAmount, double annualInterestRate, int loanTerm) {
        return creditAmount + (creditAmount * annualInterestRate * loanTerm) / 100;
    }

    private static void calculateLoan(Scanner scanner) {
        System.out.println("Выберите вид кредитного калькулятора:");
        System.out.println("1) Аннуитетный");
        System.out.println("2) Дифференцированный");
        System.out.println();
        int choice = scanner.nextInt();
        scanner.nextLine(); // Очистка буфера после считывания числа

        switch (choice) {
            case 1:
                calculateAndDisplayAnnuityPayment(scanner);
                break;
            case 2:
                calculateAndDisplayDifferentiatedPayment(scanner);
                break;
            default:
                System.out.println("Некорректный выбор");
        }
    }

    private static void calculateAndDisplayAnnuityPayment(Scanner scanner) {
        try {
            DecimalFormat df = new DecimalFormat("#.##");

            System.out.println("Введите данные для аннуитетного кредитного калькулятора:");
            System.out.println();
            System.out.print("Сумма кредита: ");
            double loanAmount = scanner.nextDouble();
            scanner.nextLine();

            System.out.print("Срок кредита (в годах): ");
            int loanTerm = scanner.nextInt();
            scanner.nextLine();

            System.out.print("Ставка (процент): ");
            double interestRate = scanner.nextDouble();
            scanner.nextLine();

            // Рассчитываем ежемесячную ставку
            double monthlyInterestRate = interestRate / 100 / 12;

            // Рассчитываем ежемесячный платеж (аннуитет)
            double monthlyPayment = loanAmount * (monthlyInterestRate / (1 - Math.pow(1 + monthlyInterestRate, -12 * loanTerm)));

            // Рассчитываем общую сумму выплаты
            double totalPayment = monthlyPayment * 12 * loanTerm;

            // Рассчитываем переплату
            double overpayment = totalPayment - loanAmount;

            // Вывод результатов
            System.out.println("1) Сумма кредита: " + loanAmount);
            System.out.println("2) Переплата: " + df.format(overpayment));
            System.out.println("3) Общая сумма выплаты: " + df.format(totalPayment));
            System.out.println("4) Процент переплаты: " + df.format(overpayment / loanAmount * 100) + "%");
            System.out.println();

            // Расчет для каждого месяца
            double remainingLoanAmount = loanAmount;
            double totalOverpayment = 0;

            for (int month = 1; month <= 12 * loanTerm; month++) {
                double monthlyInterestPayment = remainingLoanAmount * monthlyInterestRate;
                double monthlyPrincipalPayment = monthlyPayment - monthlyInterestPayment;

                // Уменьшаем оставшуюся сумму кредита
                remainingLoanAmount -= monthlyPrincipalPayment;

                // Увеличиваем общую переплату
                totalOverpayment += monthlyInterestPayment;

                // Вывод результатов для каждого месяца
                System.out.println("Месяц " + month + ":");
                System.out.println("Ежемесячный платеж: " + df.format(monthlyPayment));
                System.out.println("Погашение основного долга: " + df.format(monthlyPrincipalPayment));
                System.out.println("Погашение процентов: " + df.format(monthlyInterestPayment));
                System.out.println("Остаток по кредиту: " + df.format(remainingLoanAmount));
                System.out.println("Общая переплата: " + df.format(totalOverpayment));
                System.out.println();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private static void calculateAndDisplayDifferentiatedPayment(Scanner scanner) {
        try {
            DecimalFormat df = new DecimalFormat("#.##");

            System.out.println("Введите данные для дифференцированного кредитного калькулятора:");
            System.out.println("");

            System.out.print("Сумма кредита: ");
            double loanAmount = scanner.nextDouble();
            scanner.nextLine();

            System.out.print("Срок кредита (в годах): ");
            int loanTerm = scanner.nextInt();
            scanner.nextLine();

            System.out.print("Ставка (процент): ");
            double interestRate = scanner.nextDouble();
            scanner.nextLine();

            // Рассчитываем ежемесячный платеж (дифференцированный)
            double monthlyBasePayment = loanAmount / (loanTerm * 12);
            double totalOverpayment = 0;

            // Вывод результатов для каждого месяца
            for (int month = 1; month <= loanTerm * 12; month++) {
                double monthlyInterestPayment = loanAmount * (interestRate / 100 / 12);
                double monthlyPayment = monthlyBasePayment + monthlyInterestPayment;

                // Уменьшаем сумму кредита
                loanAmount -= monthlyBasePayment;

                // Подсчитываем переплату
                totalOverpayment += monthlyInterestPayment;

                // Вывод результатов для каждого месяца
                System.out.println("Месяц " + month + ":");
                System.out.println("Ежемесячный платеж: " + df.format(monthlyPayment));
                System.out.println("Остаток по кредиту: " + df.format(loanAmount));
                System.out.println("Переплата: " + df.format(totalOverpayment));
                System.out.println();
            }

            // Вывод общей переплаты
            System.out.println("Общая переплата: " + df.format(totalOverpayment));
            System.out.println();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void displayAnalytics(Connection connection) {
        try {
            String analyticsQuery = "SELECT * FROM Credit";
            try (PreparedStatement analyticsStatement = connection.prepareStatement(analyticsQuery);
                 ResultSet resultSet = analyticsStatement.executeQuery()) {

                System.out.println("Аналитика кредитов:");
                System.out.println();


                double totalRevenue = 0;
                int creditCount = 0;
                double totalLoanAmount = 0;

                while (resultSet.next()) {
                    String client = resultSet.getString("Client");
                    double amount = resultSet.getDouble("loan_amount");
                    double annualInterestRate = resultSet.getDouble("Annual_interest_rate");
                    int loanTerm = resultSet.getInt("Loan_term_years");
                    String loanDate = resultSet.getString("Loan_Date");
                    Date repaymentDate = resultSet.getDate("Repayment_date");
                    double revenue = resultSet.getDouble("Revenue");

                    // Вывод информации по каждому кредиту
                    System.out.println("Client: " + client);
                    System.out.println("loan_amount: " + amount);
                    System.out.println("Annual_interest_rate: " + annualInterestRate);
                    System.out.println("Loan_term_years: " + loanTerm);
                    System.out.println("Loan_Date: " + loanDate);
                    System.out.println("Repayment_date: " + repaymentDate);
                    System.out.println("Revenue: " + revenue);
                    System.out.println("---------------------");

                    // Суммирование для общих данных
                    totalRevenue += (revenue - amount); // Расчет итоговой выручки
                    creditCount++;
                    totalLoanAmount += amount;
                }

                // Вывод общих данных
                System.out.println("Итоговая полученная сумма (все выручка): " + totalRevenue);
                System.out.println("Количество кредитов: " + creditCount);
                System.out.println("Сумма всех сумм кредитов: " + totalLoanAmount);
                System.out.println();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
