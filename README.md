# FantaMaster ⚽

**FantaMaster** è un'applicazione Java progettata per la gestione completa di leghe di Fantacalcio. Il sistema permette agli utenti di creare leghe, partecipare ad aste in tempo reale, gestire la propria rosa, schierare formazioni e visualizzare i risultati basati su prestazioni reali.

##  Caratteristiche Principali

### 1. Gestione Utente e Sessioni
* **Registrazione e Login:** Sistema sicuro con hashing delle password tramite `BCrypt`.
* **Profilo:** Personalizzazione del profilo utente e caricamento avatar.
* **Sessioni:** Gestione persistente della sessione utente tramite file locali (`.sessions`).

### 2. Gestione Leghe
* **Creazione Leghe:** Definizione di regole personalizzate (bonus/malus), modalità di gioco e budget iniziale.
* **Iscrizione:** Accesso alle leghe tramite codici di invito univoci.
* **Admin Dashboard:** Strumenti per gli amministratori per gestire le impostazioni e le iscrizioni.

### 3. Mercato e Asta
* **Asta a Busta Chiusa:** Sistema di offerte segrete per l'aggiudicazione dei calciatori.
* **Proposta Giocatori:** Meccanismo a turni per chiamare i giocatori dal "listone".
* **Scambi:** Sistema di trading tra fantallenatori con validazione automatica dei ruoli.

### 4. Gestione Squadra e Formazione
* **Rosa:** Visualizzazione dettagliata dei propri calciatori con statistiche e valori.
* **Schieramento:** Interfaccia intuitiva per scegliere titolari e panchina rispettando i moduli consentiti (es. 4-4-2, 3-4-3, 5-3-2).

### 5. Simulazione e Dati Reali
* **Calcolo Punteggi:** Algoritmo automatico che applica bonus e malus definiti dalla lega alle prestazioni reali.
* **Classifiche:** Visualizzazione in tempo reale della classifica generale e dello storico punteggi.

## 🛠 Stack Tecnologico

* **Linguaggio:** Java 21
* **Interfaccia Grafica:** JavaFX (con FXML e CSS per lo styling)
* **Build Tool:** Maven
* **Database:** * **MySQL:** Per la persistenza dei dati in produzione.
    * **H2 Database:** Utilizzato per i test d'integrazione e unitari (in-memory).
* **Testing:** JUnit 4/5, TestFX (per UI Testing), Jacoco (per la coverage).
* **Qualità del Codice:** Analisi tramite SonarQube.

## 📂 Struttura del Progetto

```text
src/main/java/it/camb/fantamaster/
├── controller/      # Logica di controllo delle viste (MVC)
├── dao/             # Data Access Object (Interazione con il DB)
├── model/           # Modelli dei dati (POJO)
│   └── campionato/  # Modelli specifici per la gestione dei match
├── util/            # Classi utility (Connessioni, Password, Sessioni)
└── Main.java        # Punto di ingresso dell'applicazione

src/main/resources/
├── fxml/            # Definizione dell'interfaccia grafica
├── css/             # Fogli di stile dell'applicazione
├── images/          # Asset grafici e loghi
├── icons/           # Icone dell'interfaccia
└── api/             # File JSON con i dati dei giocatori e campionato