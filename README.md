# Automatizare si Scripting

Acest repository contine scripturi de automatizare si scripting scrise in Bash.

## Structura Repository-ului

Repository-ul este organizat in foldere pentru diferite teme sau exercitii:

### lab01

Scripturi Bash pentru exercitiul IW01 - include script pentru curatarea fisierelor temporare. Scriptul permite eliminarea fisierelor temporare dintr-un director specificat, cu suport pentru extensii personalizate.

**Caracteristici principale:**

- Eliminarea fisierelor temporare dintr-un director specificat
- Suport pentru extensii multiple de fisiere (.tmp, .log, etc.)
- Afisarea confirmarii pentru fiecare fisier sters
- Gestionarea erorilor pentru permisiuni sau fisiere inexistente

**Vezi:** [lab01/README.md](lab01/README.md)

### lab02

Script Python pentru exercitiul IW02 - client API pentru cursuri de schimb valutar. Include functionalitati pentru obtinerea cursurilor de schimb intre diferite monede, salvarea datelor in format JSON, si gestionarea erorilor.

**Caracteristici principale:**

- Obtinerea cursurilor de schimb intre oricare doua monede suportate pentru o data specifica
- Salvarea datelor in fisiere JSON cu nume descriptive
- Gestionarea cuprinzatoare a erorilor si logging
- Validarea intervalelor de date (2025-01-01 pana la 2025-09-15)
- Interfata de linie de comanda cu argumente utile

**Monede suportate:** MDL, USD, EUR, RON, UAH

**Vezi:** [lab02/README.md](lab02/README.md)

### lab03

Automatizare folosind cron pentru exercitiul IW03 - monitorizare automata a cursurilor de schimb valutar. Include configurarea unui container Docker cu cron pentru executarea periodica a scriptului de la lab02.

**Caracteristici principale:**

- Executie automata zilnica la ora 6:00 pentru cursul MDL la EUR (ziua precedenta)
- Executie automata saptamanala vineri la ora 17:00 pentru cursul MDL la USD (saptamana precedenta)
- Container Docker cu Python si cron
- Logging complet al tuturor executiilor
- Persistenta datelor in volum Docker
- Monitorizare in timp real a log-urilor cron

**Tehnologii folosite:** Docker, Docker Compose, Cron, Python

**Vezi:** [lab03/README.md](lab03/README.md)
