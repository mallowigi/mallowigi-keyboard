# Sort all keymap XML files
sort-keymaps:
	python3 scripts/sort_keymaps.py src/main/resources/keymaps/*.xml

# Compare two keymap files (usage: make compare file1=path/to/file1 file2=path/to/file2)
compare:
	python3 scripts/compare_keymaps.py $(file1) $(file2)

# Generate a diff markdown report (usage: make generate-diff file1=path/to/file1 file2=path/to/file2)
generate-diff:
	python3 scripts/generate_diff_md.py $(file1) $(file2) > diff.md
