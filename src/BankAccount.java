import java.math.BigDecimal;
import java.util.Objects;

public class BankAccount {
    private static final long TRANSFER_ONCE_LIMIT_IN_KOPECKS = 100_000_000L; /*Разовый лимит перевода(указан в копейках).*/
    private static final long TRANSFER_DAILY_LIMIT_IN_KOPECKS = 300_000_000L; /*Дневной лимит перевода(указан в копейках).*/
    private static final int CASHBACK_MIN_PERCENT = 0;
    private static final int CASHBACK_MAX_PERCENT = 10;


    private final String accountNumber;//номер аккаунта
    private final String ownerName;//имя владельца аккаунта
    private Balance balance;//баланс
    private int cashbackPercentage;//процент кэшбека, оставил не final, тк часто меняется, наверно можно реализовать по-другому
    private Balance cashback;//накопленный кэшбек
    private long transferredKopecksToday = 0;//сумма переводов за день(имеет ограничение)


    /*Конструктор. Создает аккаунт с балансом 0 в указанной валюте, с указанным процентом кэшбека */
    public BankAccount(final String accountNumber, final String ownerName, final Currency currency, final int cashbackPercentage) {
        //вообще по-хорошему он наверно должен генерироваться сам, а не указываться
        if (accountNumber == null) {
            throw new NullPointerException("Не указан номер аккаунта");
        }

        //хочу, чтобы был ровно из 20 цифр
        if (!accountNumber.matches("\\d{20}")) {
            throw new IllegalArgumentException("Номер аккаунта должен состоять из 20 цифр, а получено: %s".formatted(accountNumber));
        }

        if (ownerName == null) {
            throw new NullPointerException("Не указано имя аккаунта");
        }
        if (currency == null) {
            throw new NullPointerException("Не указана валюта для аккаунта");
        }
        //допустим у банка такие допустимые границы
        if (!isCashbackPercentageGood(cashbackPercentage)) {
            throw new IllegalArgumentException("Некорректные процент по кэшбеку");
        }

        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = new Balance(0, currency);//по моим соображениям, обычно при создании аккаунта баланс 0, потом можно пополнить
        this.cashbackPercentage = cashbackPercentage;
        this.cashback = new Balance(0, currency);
    }

    /*Можно сказать искусственный метод, просто как имитация зачисления средств, например с банкомата*/
    public void deposit(final long transferAmount, final Currency currency) {
        this.balance = this.balance.add(transferAmount, currency);
    }


    /*Метод перевода средств на определенный банковский аккаунт*/
    public void transferMoneyTo(final BankAccount accTo, final long transferAmount, final Currency currency) {
        if (accTo == null) {
            throw new NullPointerException("Невозможно совершить перевод, не указан получатель!");
        }
        if (currency == null) {
            throw new NullPointerException("Невозможно совершить перевод. Не указана валюта перевода");
        }
        if (this == accTo) {
            throw new IllegalArgumentException("Невозможно выполнить перевод самому себе");
        }
        if (transferAmount <= 0) {
            throw new IllegalArgumentException("Невозможно совершить перевод из-за некорректной суммы");
        }


        long amMultRubKopecks;
        try {
            amMultRubKopecks = Math.multiplyExact(transferAmount, currency.getPriceInRublesKopecks());
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Сумма %s слишком велика для конвертации".formatted((new Balance(transferAmount, currency)).toString()), e);
        }
        if (amMultRubKopecks / this.balance.getCurrency().getPriceInRublesKopecks() > this.balance.getAmount()) {
            throw new IllegalStateException("Невозможно совершить перевод, недостаточно средств");
        }


        long transferAmountInKopecks = amMultRubKopecks / Currency.RUB.getPriceInRublesKopecks();
        if (transferAmountInKopecks > TRANSFER_ONCE_LIMIT_IN_KOPECKS) {
            throw new IllegalStateException("Невозможно совершить перевод, превышен разовый лимит");
        }


        long newTransferredKopecksToday;
        try {
            newTransferredKopecksToday = Math.addExact(this.transferredKopecksToday, transferAmountInKopecks);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Слишком большая сумма переводов за день. Ошибка переполнения.");
        }


        if (newTransferredKopecksToday > TRANSFER_DAILY_LIMIT_IN_KOPECKS) {
            throw new IllegalStateException("Невозможно совершить перевод, превышен дневной лимит");
        }

        /*Сначала создаем отдельно новые балансы, в случае выброса исключения, никакие изменения
         * не коснутся аккаунты отправителя и получателя*/
        Balance balanceFrom = this.balance.subtract(transferAmount, currency);
        Balance balanceTo = accTo.balance.add(transferAmount, currency);
        /*Пусть за обычный перевод будет также доступно начисление кэшбека*/
        long transefrAmountMultCashbackPercentage;
        try {
            transefrAmountMultCashbackPercentage = Math.multiplyExact(transferAmount, cashbackPercentage);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Слишком большая сумма перевода. Ошибка переполнения при начислении кэшбека");
        }
        Balance newCashback = cashback.add(transefrAmountMultCashbackPercentage / 100, currency);


        this.balance = balanceFrom;
        this.cashback = newCashback;
        accTo.balance = balanceTo;

        this.transferredKopecksToday = newTransferredKopecksToday;
    }

    /*метод сброса суммы переводов за день */
    public void refreshDailyLimit() {
        this.transferredKopecksToday = 0;
    }

    /*Обналичивание накопленного кэшбека*/
    public void cashbackEncash() {
        if (this.cashback.getAmount() == 0) {
            throw new IllegalStateException("Ошибка. Нет накопленного кэшбека");
        }
        this.balance = this.balance.add(this.cashback.getAmount(), this.cashback.getCurrency());
        this.cashback = this.cashback.subtract(this.cashback.getAmount(), this.cashback.getCurrency());
    }

    /*Проверка, что процент кэшбека задан в адекватных значениях*/
    private static boolean isCashbackPercentageGood(final int cashbackPercentageChecked) {
        return (cashbackPercentageChecked >= CASHBACK_MIN_PERCENT && cashbackPercentageChecked <= CASHBACK_MAX_PERCENT);
    }


    public void setCashbackPercentage(final int cashbackPercentage) {
        if (!isCashbackPercentageGood(cashbackPercentage)) {
            throw new IllegalArgumentException("Некорректный процент по кэшбеку");
        }
        this.cashbackPercentage = cashbackPercentage;
    }


    public String getOwnerName() {
        return ownerName;
    }

    public Balance getBalance() {
        return balance;
    }

    public int getCashbackPercentage() {
        return cashbackPercentage;
    }

    public Balance getCashback() {
        return cashback;
    }

    public long getTransferredKopecksToday() {
        return transferredKopecksToday;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BankAccount that = (BankAccount) o;
        return Objects.equals(accountNumber, that.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(accountNumber);
    }

    @Override
    public String toString() {
        BigDecimal transferedTodayInBigDecimal = BigDecimal.valueOf(this.transferredKopecksToday, 2);

        return """
                Номер аккаунта:  **** %s | %s | %s | (кэшбэк: %s | ставка: %s%%) |
                Переведено сегодня: %s %s
                """.formatted(this.accountNumber.substring(accountNumber.length() - 4,
                        accountNumber.length()), this.ownerName, this.balance.toString(),
                cashback.toString(), cashbackPercentage, transferedTodayInBigDecimal.toPlainString(),
                Currency.RUB.getSymbol());
    }


}
