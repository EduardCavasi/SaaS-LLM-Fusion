# LaTeX Documentation Compilation Guide

This directory contains two LaTeX reports for the Verified Meeting Scheduler project:

1. **report_runtime_verification.tex** - Runtime Verification implementation report
2. **report_z3_theorem_proving.tex** - Z3 SMT Solver implementation report

## Prerequisites

To compile these LaTeX documents, you need:

- A LaTeX distribution (TeX Live, MiKTeX, or MacTeX)
- Required packages (usually installed automatically):
  - `pgf-umlcd` - For UML diagrams
  - `tikz` - For drawing diagrams
  - Standard packages: `graphicx`, `amsmath`, `listings`, `xcolor`, `hyperref`, `geometry`, `float`, `enumitem`

## Installation of pgf-umlcd

If `pgf-umlcd` is not available in your LaTeX distribution, you can install it manually:

1. Download from: https://ctan.org/pkg/pgf-umlcd
2. Or use: `tlmgr install pgf-umlcd` (TeX Live) or `miktex install pgf-umlcd` (MiKTeX)

## Compilation

### Using pdflatex:

```bash
pdflatex report_runtime_verification.tex
pdflatex report_runtime_verification.tex  # Run twice for references

pdflatex report_z3_theorem_proving.tex
pdflatex report_z3_theorem_proving.tex  # Run twice for references
```

### Using latexmk (recommended):

```bash
latexmk -pdf report_runtime_verification.tex
latexmk -pdf report_z3_theorem_proving.tex
```

## Alternative: Simplified UML Diagrams

If `pgf-umlcd` is not available, you can replace the UML diagrams with simpler TikZ diagrams or use online tools like:

- Draw.io (https://app.diagrams.net/)
- PlantUML (https://plantuml.com/)
- Lucidchart

Then include the diagrams as images:

```latex
\begin{figure}[H]
\centering
\includegraphics[width=0.8\textwidth]{class_diagram.png}
\caption{Class Diagram}
\end{figure}
```

## Document Structure

Both reports follow the required structure:

1. **Title, Author, Abstract, Team** - Project metadata
2. **Design** - UML diagrams (Use Case, Class, Deployment)
3. **Implementation** - Source code listings
4. **Experimental Results** - Test cases and performance metrics
5. **References** - Academic and technical references

## Customization

Before compiling, please update:

1. **Author names** in both documents (replace "Student Name")
2. **Team member names** in the Team section
3. Any additional implementation details specific to your work

## Notes

- The code listings use Java syntax highlighting
- UML diagrams use TikZ/pgf-umlcd - if compilation fails, consider using simpler diagrams
- Both documents share the same deployment diagram (common infrastructure)
- Source code is included from the actual implementation files

## Troubleshooting

### Error: "pgf-umlcd.sty not found"
- Install the package as described above
- Or comment out UML diagrams and use image files instead

### Error: "Undefined control sequence" in TikZ
- Ensure all required packages are installed
- Try compiling with `pdflatex` instead of `xelatex` or `lualatex`

### Code listings not showing properly
- Ensure `listings` package is installed
- Check that Java syntax is supported (it should be by default)

