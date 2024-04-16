package io.project.RegistrationForWorkshops.service;

public enum Messages {
    START_MESSAGE("""
            Я могу помочь записаться на практикумы по высшей математике
            Вызови команду /register, чтобы зарегистрировать свои данные
            """),
    NAME_REQUEST_MESSAGE("""
            Введи свои данные в формате Фамилия Имя Отчество
            Например: Райгородский Андрей Михайлович
            """),
    GROUP_REQUEST_MESSAGE("""
            Напиши свою группу
            Например: Б05-321
            """),
    EMAIL_REQUEST_MESSAGE("""
            Напиши свою физтех почту
            Например: ivanov.ii@phystech.edu
            """),
    INCORRECT_FORMAT("""
            Неправильный формат. Попробуй снова
            Введи /cancel для отмены операции
            """),
    REGISTRATION_FINISHED("""
            Регстрация успешно завершена
            """),
    ALREADY_REGISTERED("""
            Ты уже зарегистрирован
            """),
    CHOOSE_SUBJECT("""
            Выбери предмет, по которому нужно отсортировать
            Или нажми записаться, после чего введи ID слота
            """),
    CHOOSE_ID("Напиши ID слота, на который хочешь записаться, и предмет через пробел");

    private final String implementation;

    Messages(String implementation) {
        this.implementation = implementation;
    }

    @Override
    public String toString() {
        return implementation;
    }
}
