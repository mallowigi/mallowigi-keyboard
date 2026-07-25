import re
import sys


def parse_keymap(content):
    action_pattern = re.compile(r'<action id="([^"]+)"(.*?)>', re.DOTALL)
    actions = {}

    matches = list(action_pattern.finditer(content))
    last_end = 0
    for i, match in enumerate(matches):
        if match.start() < last_end:
            continue

        action_id = match.group(1)
        attributes_and_closing = match.group(2)

        if attributes_and_closing.strip().endswith('/'):
            # Self-closing
            actions[action_id] = None
            last_end = match.end()
        else:
            # Has a closing tag </action>
            end_tag = "</action>"
            start_pos = match.end()
            end_pos = content.find(end_tag, start_pos)
            if end_pos != -1:
                inner_content = content[match.end():end_pos].strip()
                actions[action_id] = inner_content
                last_end = end_pos + len(end_tag)
            else:
                actions[action_id] = None
                last_end = match.end()

    return actions


def get_shortcuts(action_content):
    if action_content is None:
        return []
    shortcuts = re.findall(r'<keyboard-shortcut first-keystroke="([^"]+)"(?: second-keystroke="([^"]+)")?\s*\/>', action_content)
    mouse_shortcuts = re.findall(r'<mouse-shortcut keystroke="([^"]+)"\s*\/>', action_content)

    res = []
    for first, second in shortcuts:
        if second:
            res.append(f"{first}, {second}")
        else:
            res.append(f"{first}")
    for mouse in mouse_shortcuts:
        res.append(f"mouse: {mouse}")
    return sorted(res)


def generate_markdown(file1_path, file2_path):
    # file1 is Mallowigi OSX.xml
    # file2 is Elior (2).xml
    with open(file1_path, 'r') as f:
        content1 = f.read()
    with open(file2_path, 'r') as f:
        content2 = f.read()

    actions1 = parse_keymap(content1)
    actions2 = parse_keymap(content2)

    all_ids = sorted(set(actions1.keys()) | set(actions2.keys()))

    print(f"# Keymap Comparison\n")
    print(f"| Action ID | Mallowigi Shortcut | Elior Shortcut | Status |")
    print(f"|---|---|---|---|")

    for aid in all_ids:
        s1 = get_shortcuts(actions1.get(aid))
        s2 = get_shortcuts(actions2.get(aid))

        s1_str = "<br>".join(s1) if s1 else "-"
        s2_str = "<br>".join(s2) if s2 else "-"

        if aid not in actions1:
            print(f"| {aid} | - | {s2_str} | Added |")
        elif aid not in actions2:
            print(f"| {aid} | {s1_str} | - | Removed |")
        elif s1 != s2:
            print(f"| {aid} | {s1_str} | {s2_str} | Changed |")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python generate_diff_md.py <Mallowigi_file.xml> <Elior_file.xml>")
        sys.exit(1)

    generate_markdown(sys.argv[1], sys.argv[2])
