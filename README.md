# Отчёт

Анализ Производительности
-----------------
Этот пул показал сопоставимые результаты с ThreadPoolExecutor при низкой и средней нагрузке.
При высокой нагрузке, пул может уступать в производительности.
Пул эффективно управляет потоками, создавая новые только при необходимости и завершая их при простое.

Мини-исследование
------------
CorePoolSize:
Оптимальное значение зависит от количества ядер процессора и характера задач

MaxPoolSize:
Слишком большое значение может привести к избыточному созданию потоков и увеличению накладных расходов

KeepAliveTime:
Короткое значение позволяет быстрее освобождать ресурсы, но может привести к частому созданию и завершению потоков

QueueSize:
Большая очередь может увеличить задержку выполнения задач

Принцип действия механизма распределения задач
---------------------
В текущей реализации используется одна общая очередь задач (BlockingQueue), из которой потоки берут задачи для выполнения
