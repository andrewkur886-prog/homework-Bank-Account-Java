import java.math.BigDecimal;
import java.util.Objects;


public final class Balance {
    private final long amount;//средства на балансе в копейках, центах и тд
    private final Currency currency;//валюта

    /*Конструктор с двумя аргументами, для полной картины сколько и в какой валюте храним*/
    public Balance(final long amount, final Currency currency) {
        if (currency == null) {
            throw new NullPointerException("Баланс не может быть создан без указания валюты");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Баланс не может быть создан с отрицательным балансом");
        }
        this.amount = amount;
        this.currency = currency;
    }

    public long getAmount() {
        return this.amount;
    }

    public Currency getCurrency() {
        return this.currency;
    }


    /*Метод конвертации валюты. Предусматривает проблему, что при конвертации определенной суммы в другую валюту
     * получается 0 из-за целочисленного поля*/
    private static long convert(long am, final Currency currencyFrom, final Currency currencyTo) {
        long amMultRubKopecks;
        try {
            amMultRubKopecks = Math.multiplyExact(am, currencyFrom.getPriceInRublesKopecks());
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Сумма %s слишком велика для конвертации".formatted((new Balance(am, currencyFrom)).toString()), e);
        }


        long result = amMultRubKopecks / currencyTo.getPriceInRublesKopecks();


        if (result == 0 && am > 0) {
            throw new IllegalStateException("""
                    Сумма %s %s слишком мала: при конвертации в %s \
                    получится 0""".formatted(am, currencyFrom.getSymbol(), currencyTo.name())
            );
        }
        return result;
    }

    /* Метод добавления определенной суммы в определенной валюте. Возвращает новый объект класса*/
    public Balance add(final long amountToAdd, final Currency currencyAdd) {
        if (currencyAdd == null) {
            throw new NullPointerException("Невозможно зачисление неизвестной валюты");
        }
        if (amountToAdd < 0) {
            throw new IllegalArgumentException("Невозможно прибавить к балансу отрицательное значение");
        }

        long newAmount = this.amount;
        try {
            if (this.currency != currencyAdd) {
                newAmount = Math.addExact(newAmount, convert(amountToAdd, currencyAdd, this.currency));//можно добавить +1 в случае convert = 0
            } else {
                newAmount = Math.addExact(newAmount, amountToAdd);
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Слишком большая сумма на балансе. Ошибка переполнения.", e);
        }
        return new Balance(newAmount, this.currency);
    }

    /* Метод вычета определенной суммы в определенной валюте. Возвращает новый объект класса*/
    public Balance subtract(final long amountToSubtract, final Currency currencySubtract) {
        if (currencySubtract == null) {
            throw new NullPointerException("Невозможно вычитание неизвестной валюты");
        }
        if (amountToSubtract < 0) {
            throw new IllegalArgumentException("Невозможно вычитание с баланса отрицательных сумм");
        }
        if (this.amount == 0) {
            throw new IllegalStateException("На балансе недостаточно средств");
        }

        long newAmount = this.amount;

        if (this.currency != currencySubtract) {
            newAmount -= convert(amountToSubtract, currencySubtract, this.currency);//можно добавить -1 в случае convert = 0
        } else {
            newAmount -= amountToSubtract;
        }

        if (newAmount < 0) {
            throw new IllegalStateException("На балансе недостаточно средств для выполнения операции");
        }

        return new Balance(newAmount, this.currency);
    }

    /*Метод конвертации средств на балансе в другую валюту*/
    public Balance convertTo(final Currency currencyTo) {
        if (currencyTo == null) {
            throw new NullPointerException("Невозможна конвертация в неизвестную валюту");
        }

        if (this.currency == currencyTo) {
            return this;
        } else {
            long newAmount = convert(this.amount, this.currency, currencyTo);
            return new Balance(newAmount, currencyTo);
        }

    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Balance balance = (Balance) o;
        return amount == balance.amount && currency == balance.currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }


    @Override
    public String toString() {
        BigDecimal amountInBigDecimal = BigDecimal.valueOf(amount, 2);
        return "%s %s".formatted(amountInBigDecimal.toPlainString(), this.currency.getSymbol());
    }

}
