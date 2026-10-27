# CONTENT SPEC — ICT Computing Track

Tu produis du contenu pédagogique pour une application Android de révision de la
Huawei ICT Competition, **Computing Track** (openEuler 50 %, openGauss 30 %, Kunpeng 20 %).

## Langues
- **Cours, objectifs, points clés, pièges, explications : français** (accents corrects, style clair et direct).
- **Questions et options du QCM : anglais** (style examen Huawei).
- Les termes techniques restent en anglais dans le texte français (ex. *systemd*, *tablespace*, *porting*).

## Fichiers à produire
Pour chaque chapitre :
`app/src/main/assets/content/<PHASE>/<Subject>/ch<NN>.json`

Et un index matière :
`app/src/main/assets/content/<PHASE>/<Subject>/index.json`

### index.json
```json
{
  "phase": "HCIA",
  "subject": "openEuler",
  "name": "openEuler",
  "tagline": "Système d'exploitation open source pour le calcul diversifié",
  "weight": 50,
  "chapters": [
    {"id": "hcia-oe-01", "file": "ch01.json", "title": "Écosystème openEuler, IA et installation", "minutes": 35}
  ]
}
```

### chNN.json
```json
{
  "id": "hcia-oe-01",
  "phase": "HCIA",
  "subject": "openEuler",
  "number": 1,
  "title_fr": "Écosystème openEuler, IA et installation",
  "title_en": "openEuler Ecosystem, AI, and Installation",
  "minutes": 35,
  "source": "HCIA-openEuler V2.0 Training Material, p. 1-54",
  "objectives": ["...", "..."],
  "sections": [
    {
      "heading": "1. Pourquoi openEuler existe",
      "body": "3 à 5 paragraphes explicatifs en français (120 à 220 mots). Explique le pourquoi, pas seulement le quoi. Donne des exemples concrets et des analogies si utile.",
      "bullets": ["Point clé concret", "..."],
      "code": ["dnf install nginx", "systemctl enable --now nginx"],
      "table": {"headers": ["Commande", "Rôle"], "rows": [["dnf", "Gestion des paquets"]]},
      "diagram": "Application\n    │\n    ▼\nNoyau Linux\n    │\n    ▼\nMatériel ARM64",
      "exercises": [
        {"prompt": "Énoncé concret en français.", "correction": "Corrigé détaillé, étapes et résultat."}
      ],
      "tip": "Conseil d'examen ou erreur fréquente, en français."
    }
  ],
  "key_points": ["5 à 8 phrases à retenir absolument"],
  "traps": ["2 à 5 pièges classiques du QCM Huawei"],
  "quiz": [
    {
      "id": "q01",
      "type": "single",
      "prompt": "Which component ...?",
      "options": ["...", "...", "...", "..."],
      "answer": [1],
      "explanation_fr": "Explication pédagogique en français : pourquoi la bonne réponse est bonne, et pourquoi les autres sont fausses.",
      "topic": "Architecture"
    },
    {
      "id": "q05",
      "type": "multi",
      "prompt": "Which of the following ...? (Choose two.)",
      "options": ["...", "...", "...", "..."],
      "answer": [0, 2],
      "explanation_fr": "...",
      "topic": "Administration"
    },
    {
      "id": "q07",
      "type": "tf",
      "prompt": "Statement ...",
      "options": ["True", "False"],
      "answer": [0],
      "explanation_fr": "...",
      "topic": "Sécurité"
    },
    {
      "id": "q09",
      "type": "text",
      "prompt": "Which command displays ...? (Enter only the command.)",
      "accept": ["df -h", "df -h /"],
      "sample_answer": "df -h",
      "explanation_fr": "...",
      "topic": "Stockage"
    }
  ]
}
```

## Règles de qualité
1. **Cours explicite** : 6 à 8 sections par chapitre, 1 000 à 1 800 mots au total. Chaque section
   explique le concept, son utilité, puis un exemple concret (commande, config, tableau ou schéma).
   Prévoir au moins 2 exercices corrigés par chapitre (`exercises`) et des schémas ASCII (`diagram`)
   lorsque cela clarifie une architecture ou un flux.
2. **Fidélité à la source** : appuie-toi sur les fichiers `content_src/...` fournis. Cite les pages
   dans `source`. N'invente pas de chiffres, de noms de produits ou de syntaxe. En cas de doute,
   reste général.
3. **QCM = exactement 20 questions par chapitre**, couvrant TOUT le chapitre :
   - au moins 6 `single`
   - au moins 2 `multi` (2 bonnes réponses ou plus)
   - au moins 2 `tf`
   - au moins 2 `text` (réponse courte sans ambiguïté : commande, nom, chiffre, concept)
   - Mélange difficultés et sections. Pas de question hors sujet.
4. **Explication française obligatoire** pour chaque question, même les faciles. Explique aussi
   pourquoi les distracteurs sont faux. 2 à 5 phrases.
5. **Anglais du QCM** : grammaire d'examen Huawei, options courtes et plausibles, pas d'indices
   grammaticaux. Les mauvaises réponses doivent être crédibles.
6. **Pas de doublons** entre chapitres. Chaque question teste une notion différente.
7. **JSON strictement valide** (UTF-8, pas de virgule finale, pas de markdown autour).
8. IDs : `"<phase minuscule>-<code matière>-<num>-q<NN>"` — openEuler=`oe`, openGauss=`og`, Kunpeng=`kp`.
   Exemple : `hcia-oe-01-q01`.

## Vérification
```bash
python3 tools/validate_content.py app/src/main/assets/content
```
Corrige jusqu'à ce que la validation passe.

## Barème de l'app
- Test de chapitre : 20 questions, seuil **60 %** ; le mode concentration reste actif jusqu'à la réussite.
- Mock matière : tirage de 30 questions, 30 min, score /1000, seuil 600.
- Examen de phase : 60 questions (HCIA) / 150 questions (HCIP) / 60 questions (HCIE),
  répartition officielle 50/30/20, chronométré, score /1000, seuil 600.
