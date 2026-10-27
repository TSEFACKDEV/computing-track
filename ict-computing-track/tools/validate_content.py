#!/usr/bin/env python3
"""Validate the content JSON of the ICT Computing Track app."""
import json, os, re, sys, unicodedata

PHASES = {"HCIA", "HCIP", "HCIE"}
SUBJECTS = {"openEuler": 50, "openGauss": 30, "Kunpeng": 20}
REQUIRED_TYPES = {"single": 6, "multi": 2, "tf": 2, "text": 2}
EXPECTED_QUESTIONS = 20
errors = []
warnings = []
stats = {"chapters": 0, "questions": 0, "by_phase": {}, "by_subject": {}, "by_type": {}}
seen_prompts = {}


def err(path, msg):
    errors.append(f"{path}: {msg}")


def warn(path, msg):
    warnings.append(f"{path}: {msg}")


def norm(s):
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode().lower()
    return re.sub(r"\s+", " ", re.sub(r"[^a-z0-9 ]", "", s)).strip()


def check_chapter(path, data, expect_id=None):
    rel = os.path.relpath(path)
    for key in ["id", "phase", "subject", "number", "title_fr", "title_en", "minutes",
                "source", "objectives", "sections", "key_points", "traps", "quiz"]:
        if key not in data:
            err(rel, f"champ manquant: {key}")
            return
    if data["phase"] not in PHASES:
        err(rel, f"phase invalide: {data['phase']}")
    if data["subject"] not in SUBJECTS:
        err(rel, f"matière invalide: {data['subject']}")
    if expect_id and data["id"] != expect_id:
        err(rel, f"id {data['id']} != {expect_id}")
    if not (3 <= len(data["objectives"]) <= 8):
        warn(rel, f"{len(data['objectives'])} objectifs (3-8 attendus)")
    if not (4 <= len(data["sections"]) <= 10):
        warn(rel, f"{len(data['sections'])} sections (5-8 attendus)")
    words = 0
    exercises = 0
    for i, sec in enumerate(data["sections"]):
        if not sec.get("heading"):
            err(rel, f"section {i}: heading vide")
        body = sec.get("body", "")
        words += len(body.split())
        if len(body.split()) < 50:
            warn(rel, f"section {i}: body court ({len(body.split())} mots)")
        tbl = sec.get("table")
        if tbl:
            if not isinstance(tbl.get("headers"), list) or not isinstance(tbl.get("rows"), list):
                err(rel, f"section {i}: table invalide")
            else:
                for row in tbl["rows"]:
                    if len(row) != len(tbl["headers"]):
                        err(rel, f"section {i}: ligne de table de mauvaise taille")
        diag = sec.get("diagram")
        if diag is not None and not isinstance(diag, str):
            err(rel, f"section {i}: diagram doit être une chaîne")
        exs = sec.get("exercises", [])
        if not isinstance(exs, list):
            err(rel, f"section {i}: exercises doit être une liste")
        else:
            exercises += len(exs)
            for j, ex in enumerate(exs):
                if not isinstance(ex, dict) or not ex.get("prompt") or not ex.get("correction"):
                    err(rel, f"section {i}: exercice {j} incomplet (prompt/correction)")
    if exercises < 2:
        warn(rel, f"seulement {exercises} exercice(s) corrigé(s) (<2)")
    if words < 900:
        warn(rel, f"cours court: {words} mots (<900)")
    if len(data["key_points"]) < 4:
        warn(rel, "moins de 4 key_points")
    if len(data["traps"]) < 2:
        warn(rel, "moins de 2 traps")

    quiz = data["quiz"]
    stats["chapters"] += 1
    stats["questions"] += len(quiz)
    stats["by_phase"][data["phase"]] = stats["by_phase"].get(data["phase"], 0) + 1
    stats["by_subject"][data["subject"]] = stats["by_subject"].get(data["subject"], 0) + 1
    if len(quiz) != EXPECTED_QUESTIONS:
        err(rel, f"{len(quiz)} questions (exactement {EXPECTED_QUESTIONS} attendues)")
    counts = {}
    for q in quiz:
        qid = q.get("id", "?")
        t = q.get("type")
        counts[t] = counts.get(t, 0) + 1
        stats["by_type"][t] = stats["by_type"].get(t, 0) + 1
        if t not in REQUIRED_TYPES:
            err(rel, f"{qid}: type invalide {t}")
            continue
        if not q.get("prompt"):
            err(rel, f"{qid}: prompt vide")
        if len(q.get("explanation_fr", "")) < 40:
            err(rel, f"{qid}: explanation_fr trop courte")
        opts = q.get("options")
        ans = q.get("answer")
        if t in ("single", "multi", "tf"):
            if not isinstance(opts, list) or len(opts) < 2:
                err(rel, f"{qid}: options invalides")
                continue
            if not isinstance(ans, list) or not ans:
                err(rel, f"{qid}: answer invalide")
                continue
            if len(set(ans)) != len(ans):
                err(rel, f"{qid}: answer contient des doublons")
            if any((not isinstance(a, int)) or a < 0 or a >= len(opts) for a in ans):
                err(rel, f"{qid}: index de réponse hors limites")
            if t == "single" and len(ans) != 1:
                err(rel, f"{qid}: single doit avoir exactement 1 réponse")
            if t == "multi" and len(ans) < 2:
                err(rel, f"{qid}: multi doit avoir au moins 2 réponses")
            if t == "tf" and opts != ["True", "False"]:
                err(rel, f"{qid}: tf options doivent être ['True','False']")
        if t == "text":
            acc = q.get("accept")
            if not isinstance(acc, list) or not acc:
                err(rel, f"{qid}: accept manquant")
            elif any(not str(a).strip() for a in acc):
                err(rel, f"{qid}: accept contient une chaîne vide")
            if not q.get("sample_answer"):
                warn(rel, f"{qid}: sample_answer manquant")
        p = norm(q.get("prompt", ""))
        if p:
            if p in seen_prompts:
                warn(rel, f"{qid}: prompt en doublon avec {seen_prompts[p]}")
            else:
                seen_prompts[p] = f"{data['id']}/{qid}"
    for t, minimum in REQUIRED_TYPES.items():
        if counts.get(t, 0) < minimum:
            err(rel, f"seulement {counts.get(t,0)} question(s) {t} (min {minimum})")


def main(root):
    manifest_path = os.path.join(root, "index.json")
    if not os.path.exists(manifest_path):
        err(manifest_path, "index.json racine manquant")
    else:
        manifest = json.load(open(manifest_path, encoding="utf-8"))
        phases = {p["id"] for p in manifest.get("phases", [])}
        for p in PHASES - phases:
            warn("index.json", f"phase absente du manifeste: {p}")
    for phase in sorted(os.listdir(root)):
        pdir = os.path.join(root, phase)
        if not os.path.isdir(pdir) or phase not in PHASES:
            continue
        for subject in sorted(os.listdir(pdir)):
            sdir = os.path.join(pdir, subject)
            if not os.path.isdir(sdir) or subject not in SUBJECTS:
                continue
            idx_path = os.path.join(sdir, "index.json")
            if not os.path.exists(idx_path):
                err(idx_path, "index matière manquant")
                continue
            try:
                idx = json.load(open(idx_path, encoding="utf-8"))
            except Exception as e:
                err(idx_path, f"JSON invalide: {e}")
                continue
            if idx.get("weight") != SUBJECTS[subject]:
                warn(idx_path, f"poids {idx.get('weight')} != {SUBJECTS[subject]}")
            for ch in idx.get("chapters", []):
                f = os.path.join(sdir, ch["file"])
                if not os.path.exists(f):
                    err(f, "fichier chapitre manquant")
                    continue
                try:
                    data = json.load(open(f, encoding="utf-8"))
                except Exception as e:
                    err(f, f"JSON invalide: {e}")
                    continue
                check_chapter(f, data)
    print(f"Chapitres: {stats['chapters']} | Questions: {stats['questions']}")
    print("Par phase:", stats["by_phase"])
    print("Par matière:", stats["by_subject"])
    print("Par type:", stats["by_type"])
    if warnings:
        print(f"\n{len(warnings)} avertissement(s):")
        for w in warnings[:60]:
            print("  WARN", w)
        if len(warnings) > 60:
            print(f"  ... et {len(warnings)-60} autres")
    if errors:
        print(f"\n{len(errors)} ERREUR(S):")
        for e in errors[:120]:
            print("  ERR ", e)
        if len(errors) > 120:
            print(f"  ... et {len(errors)-120} autres")
        return 1
    print("\nContenu valide.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/content"))
