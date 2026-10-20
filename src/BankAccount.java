import java.math.BigDecimal;
import java.util.Objects;

public class BankAccount
{
    private final String accountNumber;//номер аккаунта
    private final String ownerName;//имя владельца аккаунта
    private Balance balance;//баланс
    private int cashbackPercentage;//процент кэшбека, оставил не final, тк часто меняется, наверно можно реализовать по-другому
    private Balance cashback;//накопленный кэшбек
    private long transferredKopecksToday = 0;//сумма переводов за день(имеет ограничение)



    /*Конструктор. Создает аккаунт с балансом 0 в указанной валюте, с указанным процентом кэшбека */
    public BankAccount(final String accountNumber,final String ownerName,final Currency currency, final int cashbackPercentage)
    {
        if(accountNumber==null)//вообще по-хорошему он наверно должен генерироваться сам, а не указываться
        {
            throw new NullPointerException("Не указан номер аккаунта");
        }
        if (!accountNumber.matches("\\d{20}"))//хочу, чтобы был ровно из 20 цифр
        {
            throw new IllegalArgumentException("Номер аккаунта должен состоять из 20 цифр, а получено: " + accountNumber);
        }
        if(ownerName == null)
        {
            throw new NullPointerException("Не указано имя аккаунта");
        }
        if(currency== null)
        {
            throw new NullPointerException("Не указана валюта для аккаунта");
        }
        if(!isCashbackPercentageGood(cashbackPercentage))//допустим у банка такие допустимые границы
        {
            throw new IllegalArgumentException("Некорректные процент по кэшбеку");
        }

        this.accountNumber = accountNumber;
        this.ownerName= ownerName;
        this.balance = new Balance(0, currency);//по моим соображениям, обычно при создании аккаунта баланс 0, потом можно пополнить
        this.cashbackPercentage = cashbackPercentage;
        this.cashback = new Balance(0, currency);
    }

    /*Можно сказать искусственный метод, просто как имитация зачисления средств, например с банкомата*/
    public void deposit(final long transferAmount, final Currency currency)
    {
        this.balance = this.balance.add(transferAmount, currency);
    }


    /*Метод перевода средств на определенный банковский аккаунт*/
    public void transferMoneyTo(final BankAccount accTo, final long transferAmount, final Currency currency)
    {
        if(accTo == null)
        {
            throw new NullPointerException("Невозможно совершить перевод, не указан получатель!");
        }
        if(currency==null)
        {
            throw new NullPointerException("Невозможно совершить перевод. Не указана валюта перевода");
        }
        if(this == accTo)
        {
            throw new IllegalArgumentException("Невозможно выполнить перевод самому себе");
        }
        if(transferAmount<=0)
        {
            throw new IllegalArgumentException("Невозможно совершить перевод из-за некорректной суммы");
        }

        if(transferAmount* currency.getPriceInRublesKopecks()/this.balance.getCurrency().getPriceInRublesKopecks()>this.balance.getAmount())
        {
            throw new IllegalStateException("Невозможно совершить перевод, недостаточно средств");
        }


        /*Разовый лимит перевода(указан в копейках). Можно сделать глобальной константой*/
        final long transferOnceLimitInKopecks = 100000000;

        long transferAmountInKopecks =transferAmount* currency.getPriceInRublesKopecks()/Currency.RUB.getPriceInRublesKopecks();
        if(transferAmountInKopecks > transferOnceLimitInKopecks)
        {
            throw new IllegalStateException("Невозможно совершить перевод, превышен разовый лимит");
        }


        /*Дневной лимит перевода(указан в копейках). Можно сделать глобальной константой*/
        long transferDailyLimitInKopecks = 300000000;

        if(this.transferredKopecksToday +transferAmountInKopecks>transferDailyLimitInKopecks)
        {
            throw new IllegalStateException("Невозможно совершить перевод, превышен дневной лимит");
        }


        /*Сначала создаем отдельно новые балансы, в случае выброса исключения, никакие изменения
        * не коснутся аккаунты отправителя и получателя*/
        Balance balanceFrom = this.balance.subtract(transferAmount, currency);
        Balance balanceTo = accTo.balance.add(transferAmount, currency);
        /*Пусть за обычный перевод будет также доступно начисление кэшбека*/
        Balance newCashback = cashback.add(transferAmount*cashbackPercentage/100, currency);



        this.balance = balanceFrom;
        this.cashback = newCashback;
        accTo.balance=balanceTo;

        this.transferredKopecksToday +=transferAmountInKopecks;
    }

    /*метод сброса суммы переводов за день */
    public void refreshDailyLimit()
    {
        this.transferredKopecksToday =0;
    }

    /*Обналичивание накопленного кэшбека*/
    public void cashbackEncash()
    {
        if(this.cashback.getAmount()==0)
        {
            throw new IllegalStateException("Ошибка. Нет накопленного кэшбека");
        }
        this.balance = this.balance.add(this.cashback.getAmount(), this.cashback.getCurrency());
        this.cashback = this.cashback.subtract(this.cashback.getAmount(), this.cashback.getCurrency());
    }

    /*Проверка, что процент кэшбека задан в адекватных значениях*/
    private static boolean isCashbackPercentageGood(final int cashbackPercentageChecked)
    {
        return (cashbackPercentageChecked >=0 && cashbackPercentageChecked <= 10);
    }


    public void setCashbackPercentage(final int cashbackPercentage)
    {
        if(!isCashbackPercentageGood(cashbackPercentage))
        {
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
    public String toString()
    {
        BigDecimal transferedTodayInBigDecimal = BigDecimal.valueOf(this.transferredKopecksToday, 2);

        return "Номер аккаунта:  **** "+ this.accountNumber.substring(accountNumber.length()-4, accountNumber.length())+"\t"
                + this.ownerName + "\t" + this.balance.toString()
                + "\t(кэшбэк: " + cashback.toString() + "\tставка: " + cashbackPercentage + "%)"
                + "\tПереведено сегодня: "
                + transferedTodayInBigDecimal.toPlainString()+ " " + Currency.RUB.getSymbol();
    }


}
