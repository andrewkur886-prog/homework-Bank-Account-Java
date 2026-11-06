public class Main {
    public static void main(String[] args) {
        /**
         * здесь блок try catch просто для вывода сообщения исключения,
         * можно было бы все аккуратно раскидать по разным
         * try catch, например если поле null, то делаем что-то дефолтное, если ошибка состояния,
         * то просто пишем об ошибке и продолжаем работать
         * если неверно указан аргумент, то добиваемся того, чтобы ввели корректный аргумент и тд
         * так же наверно показаны далеко не все случаи исключения
         * Сам по себе он очень грязный получился, если нужно аккуратнее
         * и показать работу вообще всех корнер-кейсов, то исправлю*/
        try {
            BankAccount client1 = new BankAccount("12345678910111213141", "Oleg Tinkov", Currency.RUB, 5);
            System.out.println(client1);

            client1.deposit(600000000, Currency.RUB);
            System.out.println(client1);

            BankAccount client2 = new BankAccount("11111222223344510874", "Vladimir Verhoshinski", Currency.USD, 4);
            System.out.println(client2);

            client2.deposit(9600, Currency.RUB);
            System.out.println(client2);

//            неправильный номер счета
//            BankAccount client3 = new BankAccount("1111122в223344510874","Vladimir Verhoshinski", Currency.USD, 4);
//            System.out.println(client2);

//            //не указано имя аккаунта
//            BankAccount client3 = new BankAccount("11111222223344510874",null, Currency.USD, 4);
//            System.out.println(client2);


            //и тд с конструктором


            client1.transferMoneyTo(client2, 100000000, Currency.RUB);
            System.out.println(client1);
            System.out.println(client2);

            client1.transferMoneyTo(client2, 100000000, Currency.RUB);
            System.out.println(client1);
            System.out.println(client2);

            //            //превышен разовый лимит
//            client1.transferMoneyTo(client2, 100000001, Currency.RUB);
//            System.out.println(client1);
//            System.out.println(client2);

            client1.transferMoneyTo(client2, 100000000, Currency.RUB);
            System.out.println(client1);
            System.out.println(client2);

//            //превышен дневной лимит
//            client1.transferMoneyTo(client2, 100000000, Currency.RUB);
//            System.out.println(client1);
//            System.out.println(client2);

            client1.refreshDailyLimit();
            System.out.println(client1);

            client1.cashbackEncash();
            System.out.println(client1);

//            //невозможно перевести самому себе
//            client2.transferMoneyTo(client2, 10000, Currency.USD);


//            //проблема из-за целочисленного long
//            client1.transferMoneyTo(client2, 1, Currency.RUB);
//            System.out.println(client1);
//            System.out.println(client2);

//            //проблема переполнения long, отлавливается multiplyExact
//            BankAccount client3 = new BankAccount("12345678910111413141", "? ?", Currency.RUB, 5);
//            client3.deposit(9_922_337_203_685_477L, Currency.USD);
//            System.out.println(client3);

//            //проблема переполнения long, отлавливается addExact
//            client1.deposit(9_223_372_036_539_776_000L, Currency.RUB);
//            System.out.println(client1);



        } catch (IllegalArgumentException | IllegalStateException | NullPointerException e) {
            System.out.println(e.getMessage());
        }

    }

}
