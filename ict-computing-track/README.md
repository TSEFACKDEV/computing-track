# ICT Computing Track — application Android

Application Android native (Kotlin + Jetpack Compose) pour réviser la **Huawei ICT Competition — Computing Track** (openEuler, openGauss, Kunpeng), hors ligne.

Projet reconstruit par rétro-ingénierie depuis :

- l'historique de la session OpenCode `ses_e9c9558e3ffetOuESThTxPxYWi` ;
- l'APK de référence `base.apk` (`reference/ict-computing-track-base.apk`, SHA-256 dans `reference/SHA256SUMS`).

## État de la reconstruction

| Élément | Résultat |
|---|---|
| Identifiant | `com.klein.ictcomputing` |
| Version | `1.0` (versionCode 1) |
| Min / target / compile SDK | 26 / 35 / 35 |
| Stack | Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00), Material 3 |
| Build | AGP 8.7.3, Gradle 8.13 |
| Assets de contenu | 44 fichiers JSON — **enrichis** (cours détaillés, 680 questions) par rapport à l'APK d'origine |
| Code | socle reconstruit à l'identique + ajouts (mode concentration, schémas, exercices) |
| Signature | même certificat debug que l'APK d'origine |
| Build vérifié | `BUILD SUCCESSFUL` — APK ~9,1 Mo dans `app/build/outputs/apk/debug/app-debug.apk` |

## Contenu pédagogique

34 chapitres, **680 questions** au total (exactement **20 par chapitre**) :

| Phase | Matière | Chapitres | Questions |
|---|---|---:|---:|
| HCIA | openEuler | 5 | 100 |
| HCIA | openGauss | 6 | 120 |
| HCIA | Kunpeng | 5 | 100 |
| HCIP | openEuler | 3 | 60 |
| HCIP | openGauss | 3 | 60 |
| HCIP | Kunpeng | 3 | 60 |
| HCIE | openEuler | 3 | 60 |
| HCIE | openGauss | 3 | 60 |
| HCIE | Kunpeng | 3 | 60 |

Chaque chapitre comporte :

- un cours détaillé en français (6 à 8 sections, 1 000 à 1 800 mots), accessible à un débutant mais calibré niveau compétition ;
- des tableaux comparatifs, des schémas ASCII et des blocs de commandes réelles ;
- **2 à 3 exercices corrigés** intégrés aux sections ;
- des points clés, des pièges d'examen et la source officielle ;
- un test de **20 questions** en anglais (choix unique, choix multiple, vrai/faux, réponse libre), avec explication en français, corrigé immédiat et seuil de validation à 60 %.

## Fonctionnalités

- **Parcours complet HCIA → HCIP → HCIE** par matière, chapitres verrouillés par progression.
- **Cours structurés** : objectifs, sections détaillées, tableaux, schémas et exercices corrigés.
- **Mode concentration (kiosque)** : tant qu'un chapitre n'est pas validé, l'application s'épingle à l'écran, masque les barres système, active « Ne pas déranger » et bloque le bouton Retour. Le mode se libère uniquement après un score ≥ 60 % au test du chapitre.
- **Quiz de fin de chapitre (20 questions)** avec correction immédiate, explication française, score et seuil de validation à 60 %.
- **Examens blancs par matière** et **examen de phase** (format inspiré de l'évaluation Huawei).
- **Réveil forcé à 3 h du matin** : alarme exacte, plein écran, sonnerie en boucle, écran verrouillé, redémarrage automatique après reboot.
- **Système de relances (“nag”)** dans la journée tant que la session quotidienne n'est pas terminée.
- **Coach mémoire / répétition espacée** : les notions ratées reviennent jusqu'à maîtrise.
- **Arène Boss**, XP, niveaux, séries, statistiques, confettis et retours haptiques.
- **100 % hors ligne** : contenu embarqué dans `assets/content`.
- Progression persistée localement en JSON dans `SharedPreferences`.

## Compiler

Prérequis : JDK 17, Android SDK (plateforme 35, build-tools 35+).

```bash
cd ict-computing-track
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # si nécessaire
./gradlew :app:assembleDebug
```

APK généré : `app/build/outputs/apk/debug/app-debug.apk`.

Installation sur un téléphone connecté :

```bash
adb install -r -d app/build/outputs/apk/debug/app-debug.apk
```

Après installation, autoriser : notifications, alarmes exactes, affichage par-dessus les autres applications, accès « Ne pas déranger » (pour le mode concentration) et désactiver l'optimisation de batterie pour que le réveil de 3 h soit fiable (l'écran de réglages de l'application guide ces étapes).

> **Mode kiosque** : l'épinglage d'écran Android reste contournable par le geste système de désépinglage. Un verrouillage total nécessite que l'application soit « device owner » (Android Enterprise), ce qui n'est pas le cas d'une installation APK classique. Le mode concentration combine donc épinglage, immersion, DND et blocage du Retour pour un usage d'examen sans distraction.

## Structure

```
app/src/main/java/com/klein/ictcomputing/
├── IctApp.kt / MainActivity.kt      # Application + activité principale
├── data/
│   ├── Models.kt                    # Modèles (chapitres, questions, progression)
│   └── Repo.kt                      # Chargement des assets, persistance, scoring, SRS
├── ui/                              # Écrans Compose (accueil, cours, quiz, examens,
│                                    # stats, réglages, coach, boss, onboarding…)
└── alarm/                           # AlarmManager, service plein écran, notifications,
                                     # relances quotidiennes, redémarrage après boot

app/src/main/assets/content/
├── index.json                       # Phases HCIA / HCIP / HCIE
└── <PHASE>/<MATIERE>/
    ├── index.json                   # Sommaire des chapitres
    └── chNN.json                    # Cours + quiz

tools/                               # Spécification et validation du contenu
reference/                           # APK d'origine conservé pour comparaison
docs/REVERSE_ENGINEERING.md          # Méthode et preuves de fidélité
```

## Validation du contenu

```bash
python3 tools/validate_content.py app/src/main/assets/content
```

Le validateur vérifie notamment : JSON valide, champs obligatoires, exactement 20 questions par chapitre, répartition minimale des types, absence de doublons d'énoncés, intégrité des tableaux, présence d'exercices corrigés et longueur minimale du cours.
