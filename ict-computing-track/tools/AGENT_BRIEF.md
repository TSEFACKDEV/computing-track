# BRIEF AUTEUR DE CONTENU

Tu produis du contenu pour une application Android **hors ligne** de révision de la
**Huawei ICT Competition — Computing Track** (openEuler 50 %, openGauss 30 %, Kunpeng 20 %).

## À lire avant de commencer
1. `CONTENT_SPEC.md` (schéma JSON exact et règles).
2. Le chapitre exemplaire `app/src/main/assets/content/HCIA/openEuler/ch01.json`.
3. Les sources officielles correspondantes (PDF Huawei) et, si présents, les extraits `content_src/<PHASE>/<Subject>/chNN.txt`.

## Ce que tu produis
- Un fichier `chNN.json` par chapitre, dans `app/src/main/assets/content/<PHASE>/<Subject>/`.
- Un `index.json` pour la matière, avec la liste ordonnée des chapitres et leur titre.
- **Ne touche à aucun autre dossier.** N'écrase pas le ch01 de HCIA openEuler (déjà fourni).

## Exigences de fond
- **Cours en français**, très explicite : 6 à 8 sections, 1 000 à 1 800 mots, avec le « pourquoi »,
  des exemples concrets, des commandes, des tableaux, des schémas ASCII (`diagram`), 2 à 3 exercices
  corrigés (`exercises`) et un encadré `tip` par section.
- **QCM en anglais** : exactement 20 questions par chapitre (6 single, 2 multi, 2 tf, 2 text minimum),
  couvrant toutes les notions du chapitre. Options crédibles, une seule bonne réponse pour
  `single`, au moins deux pour `multi`.
- **Explication en français** pour chaque question : pourquoi la bonne réponse est bonne,
  et pourquoi les distracteurs sont faux.
- **Fidélité** : appuie-toi sur les pages fournies. Cite la plage de pages dans `source`.
  Pas de chiffres ou de syntaxe inventés. Si une info n'est pas dans la source, reste sur des
  principes généraux vérifiables.
- Les QCM sont **originaux** (inspirés du style Huawei), pas des copies de dumps.
- JSON strictement valide, UTF-8, accents français.

## Vérification obligatoire
```bash
cd /home/klein/Desktop/Computing-track/ict-computing-track
python3 tools/validate_content.py app/src/main/assets/content
```
Corrige les erreurs de TES fichiers. Les avertissements sur les autres matières ne te concernent pas.
Vérifie aussi :
```bash
python3 -c "import json;d=json.load(open('app/src/main/assets/content/<PHASE>/<Subject>/index.json'));print(d['chapters'])"
```
