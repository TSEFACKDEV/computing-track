#!/usr/bin/env python3
"""Split the official Huawei training PDFs into per-chapter source files.

Output: content_src/<PHASE>/<Subject>/chNN.txt
Each file keeps page markers and slide text so content authors can cite pages.
"""
import os, re, sys

ROOT = os.path.dirname(os.path.abspath(__file__))
EXTRACT = "/home/klein/.tmp/opencode/extract"

PLAN = {
    "HCIA": {
        "openEuler": ("hcia-openeuler", [
            ("01", 1, 54, "Ecosysteme openEuler, IA et installation"),
            ("02", 55, 153, "Ligne de commande, fichiers et edition de texte"),
            ("03", 154, 251, "Administration systeme : utilisateurs, logiciels, services, processus"),
            ("04", 252, 320, "Stockage, volumes logiques et reseau"),
            ("05", 321, 441, "Scripts shell et conteneurs iSula"),
        ]),
        "openGauss": ("hcia-opengauss", [
            ("01", 1, 72, "Vue d'ensemble et architecture openGauss"),
            ("02", 73, 130, "Installation et deploiement"),
            ("03", 131, 272, "Architecture, objets, tablespaces et partitions"),
            ("04", 273, 351, "Gestion des bases et des objets"),
            ("05", 352, 473, "SQL : bases et requetes"),
            ("06", 474, 531, "GaussDB et services cloud"),
        ]),
        "Kunpeng": ("hcia-kunpeng-fr", [
            ("01", 10, 30, "Ecosysteme Kunpeng et architecture ARM"),
            ("02", 31, 55, "Portage et migration d'applications"),
            ("03", 56, 75, "Tests et optimisation de performance"),
            ("04", 76, 92, "Experience complete : plateforme de messagerie"),
            ("05", 93, 118, "Solutions Kunpeng et communaute"),
        ]),
    },
    "HCIP": {
        "openEuler": ("hcip-openeuler", [
            ("01", 1, 114, "Gestion des services openEuler"),
            ("02", 115, 191, "Architecture de cluster et haute disponibilite"),
            ("03", 192, 260, "Gestion du stockage"),
            ("04", 261, 404, "Scripts shell avances et automatisation"),
            ("05", 405, 559, "Durcissement securite, supervision et pratique"),
        ]),
        "openGauss": ("hcip-opengauss", [
            ("01", 1, 54, "Architecture produit openGauss"),
            ("02", 55, 112, "Gestion avancee"),
            ("03", 113, 188, "Gestion du systeme de base de donnees"),
            ("04", 189, 327, "SQL avance"),
            ("05", 328, 436, "Migration de bases de donnees"),
            ("06", 437, 530, "Securite des bases de donnees"),
        ]),
        "Kunpeng": (None, [
            ("01", 0, 0, "Kunpeng DevKit : portage et analyse"),
            ("02", 0, 0, "BoostKit et acceleration des applications"),
            ("03", 0, 0, "Optimisation avancee CPU, memoire, reseau et I/O"),
            ("04", 0, 0, "Conteneurs, virtualisation et cloud sur Kunpeng"),
            ("05", 0, 0, "Conception de solutions et etudes de cas"),
        ]),
    },
    "HCIE": {
        "openEuler": ("hcie-openeuler", [
            ("01", 1, 208, "Stockage distribue Ceph"),
            ("02", 209, 345, "Kubernetes : cluster, reseau, stockage"),
            ("03", 346, 456, "Kubernetes avance : scheduling, autoscaling, optimisation"),
            ("04", 457, 624, "CI/CD, Jenkins et automatisation avancee"),
            ("05", 625, 846, "OpenStack, observabilite et O&M intelligente"),
        ]),
        "openGauss": ("hcie-opengauss", [
            ("01", 1, 96, "Gestion des transactions"),
            ("02", 97, 278, "Stockage et journaux"),
            ("03", 279, 332, "Moteur SQL"),
            ("04", 333, 447, "Optimisation de base de donnees"),
            ("05", 448, 672, "Sauvegarde, fonctionnalites avancees et depannage"),
        ]),
        "Kunpeng": (None, [
            ("01", 0, 0, "Architecture Kunpeng expert et analyse bas niveau"),
            ("02", 0, 0, "Portage expert et performance"),
            ("03", 0, 0, "Optimisation systeme et applicative"),
            ("04", 0, 0, "Conception de solutions Haute Performance"),
            ("05", 0, 0, "Etude de cas HCIE Computing Track"),
        ]),
    },
}

NOISE = re.compile(
    r"(copyright\s*©|all rights reserved|huawei confidential|thank you|"
    r"把数字世界|bring digital to every|machine translated by google|"
    r"^\s*\d+\s*$|^\s*page\s*\d+.*$)", re.I)


def clean_page(page: str) -> str:
    out = []
    for line in page.splitlines():
        s = line.rstrip()
        if not s.strip():
            continue
        if NOISE.search(s.strip()):
            continue
        out.append(s)
    return "\n".join(out)


def main():
    for phase, subjects in PLAN.items():
        for subject, (src, chapters) in subjects.items():
            outdir = os.path.join(ROOT, "content_src", phase, subject)
            os.makedirs(outdir, exist_ok=True)
            if src is None:
                continue
            path = os.path.join(EXTRACT, src + ".txt")
            pages = open(path, encoding="utf-8", errors="ignore").read().split("\f")
            for num, start, end, title in chapters:
                body = []
                for p in range(start, end + 1):
                    if p <= len(pages):
                        c = clean_page(pages[p - 1])
                        if c.strip():
                            body.append(f"\n===== SOURCE PAGE {p} =====\n{c}")
                text = f"# {phase} {subject} - Chapitre {num}: {title}\n" + "\n".join(body)
                with open(os.path.join(outdir, f"ch{num}.txt"), "w", encoding="utf-8") as f:
                    f.write(text)
                print(phase, subject, num, len(text), "chars")

    # Manifest of source files
    with open(os.path.join(ROOT, "content_src", "PLAN.md"), "w", encoding="utf-8") as f:
        f.write("# Plan de contenu\n\n")
        for phase, subjects in PLAN.items():
            f.write(f"## {phase}\n")
            for subject, (src, chapters) in subjects.items():
                f.write(f"\n### {subject}\n")
                for num, start, end, title in chapters:
                    f.write(f"- ch{num}: {title} (p. {start}-{end})\n")


if __name__ == "__main__":
    main()
