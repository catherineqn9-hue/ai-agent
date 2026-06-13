from pathlib import Path


def test_project_standard_docs_exist():
    for path in [
        "AGENTS.md",
        ".editorconfig",
        "pyproject.toml",
        "docs/engineering-standards.md",
        "docs/project-structure.md",
    ]:
        assert Path(path).exists()


def test_project_structure_docs_define_core_layers():
    content = Path("docs/project-structure.md").read_text(encoding="utf-8")

    for layer in ["api", "dto", "services", "repositories", "agent", "core"]:
        assert layer in content


def test_backend_layer_directories_exist():
    for path in [
        "app/api/v1",
        "app/core",
        "app/dto",
        "app/services",
        "app/repositories",
        "app/agent/nodes",
        "app/agent/tools",
    ]:
        assert Path(path).is_dir()


def test_main_app_is_only_composition_root():
    content = Path("app/main.py").read_text(encoding="utf-8")

    assert "include_router" in content
    assert "@app.post" not in content
    assert "@app.put" not in content
    assert "@app.delete" not in content
