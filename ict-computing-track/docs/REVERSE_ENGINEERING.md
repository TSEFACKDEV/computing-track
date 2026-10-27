# Reconstruction — méthode et vérifications

## Source de vérité

Deux artefacts ont été utilisés :

1. **L'historique de la session** `ses_e9c9558e3ffetOuESThTxPxYWi` (base OpenCode `~/.local/share/opencode/opencode.db`).
2. **L'APK de référence** `/home/klein/Downloads/Telegram Desktop/base.apk`, copié dans `reference/`.

La session contenait 704 messages et 224 opérations d'écriture/édition. Les outils `write` et `edit` enregistrent le contenu complet des fichiers et les diffs exacts, ce qui permet de rejouer la création du projet sans décompiler l'APK.

## Procédure suivie

1. Lecture de `session_v2` et `session_message` (schéma v2 de la base OpenCode).
2. Extraction de toutes les opérations `write` / `edit` dans l'ordre des `seq`.
3. Rejeu sur le nouvel emplacement `ict-computing-track/`.
4. Rejeu des **13 commandes shell** qui modifiaient aussi les sources (corrections d'imports, manifeste, thème, padding, etc.). Ces commandes n'apparaissaient pas comme des appels `write`/`edit`.
5. Récupération des **assets finaux** directement depuis `base.apk` (`assets/content/**`), car les derniers ajouts de questions et corrections d'explications ont été faits par des scripts de fusion successifs.
6. Correction des 8 éditions dont le contexte avait changé entre-temps (correspondance exacte puis normalisation des apostrophes/tirets/espaces).
7. Build complet avec Gradle 8.13 + AGP 8.7.3.

## Vérifications de fidélité

### Assets

```text
diff -rq app/src/main/assets /tmp/apkwork/assets
→ aucune différence
```

Les 44 fichiers JSON de contenu sont **identiques octet pour octet** à ceux de `base.apk`.

### Code compilé

Comparaison des 7 fichiers DEX avec `dexdump` :

| DEX de référence | Classes | Équivalent reconstruit | Classes |
|---|---:|---|---:|
| classes.dex | 9556 | classes.dex | 9556 |
| classes2.dex | 81 | classes2.dex | 81 |
| classes3.dex | 4 | classes5.dex | 4 |
| classes4.dex | 2577 | classes7.dex | 2577 |
| classes5.dex | 26 | classes3.dex | 26 |
| classes6.dex | 40 | classes4.dex | 40 |
| classes7.dex | 276 | classes6.dex | 276 |

- `classes.dex` est **identique** (même SHA-256), donc le gros du code est identique.
- Les ensembles de descripteurs de classes de chaque DEX sont identiques.
- Les petits DEX sont regroupés dans un ordre différent par le build incrémental ; les classes sont les mêmes, seuls l'ordre et quelques métadonnées changent.

### Manifeste et ressources

- Même `applicationId`, même version, mêmes permissions, mêmes composants.
- `resources.arsc` et `AndroidManifest.xml` ont exactement la même taille que dans l'APK de référence.
- Même certificat de signature debug (`CN=Android Debug`, SHA-256 `6298a4ff…2443`), donc `adb install -r` fonctionne sans désinstaller l'ancienne version.

### Build

```text
BUILD SUCCESSFUL in 1m 25s
app/build/outputs/apk/debug/app-debug.apk  (~8,8 Mo)
```

## Reproductibilité

```bash
cd ict-computing-track
printf 'sdk.dir=%s/Android/Sdk\n' "$HOME" > local.properties
./gradlew :app:assembleDebug
```

Pour rejouer la reconstruction depuis l'historique OpenCode, les étapes sont décrites ci-dessus ; le script de rejeu utilisé pendant cette intervention n'est pas conservé dans le projet (il dépend de la base OpenCode locale), mais tous les fichiers produits sont présents et compilent.

## Écarts assumés

- **Signature** : l'APK reconstruit est signé avec la même clé debug, mais pas bit-à-bit identique (horodatage ZIP, ordre de compression).
- **Petits DEX** : ordre de fusion différent, ensembles de classes identiques.
- **Évolutions post-reconstruction** : le mode concentration (kiosque), les schémas, les exercices corrigés et l'extension à 20 questions par chapitre sont des ajouts délibérés, absents de `base.apk`. Le socle reconstruit (code, ressources, signature) reste vérifié comme décrit ci-dessus.

## Évolutions ajoutées après la reconstruction

| Élément | Implémentation |
|---|---|
| Mode concentration | `focus/FocusMode.kt` : `startLockTask()`, immersion, filtre DND, blocage du Retour, reprise dans `onResume`, libération après réussite du quiz |
| Schémas de cours | champ `diagram` dans `CourseSection`, rendu monospace défilable |
| Exercices corrigés | `CourseExercise(prompt, correction)`, cartes « énoncé / corrigé » |
| Tests de 20 questions | `tools/validate_content.py` impose exactement 20 questions et une répartition minimale des types |
| Contenu enrichi | 9 lots (phase × matière) : sections détaillées, tableaux, schémas, exercices, explications de distracteurs |

Les assets de contenu ne sont donc **plus identiques** à `base.apk` : la comparaison octet pour octet vaut pour l'état reconstruit initial, avant enrichissement.
