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
            res.append(f"keyboard: {first}, {second}")
        else:
            res.append(f"keyboard: {first}")
    for mouse in mouse_shortcuts:
        res.append(f"mouse: {mouse}")
    return sorted(res)


def compare(file1_path, file2_path):
    with open(file1_path, 'r') as f:
        content1 = f.read()
    with open(file2_path, 'r') as f:
        content2 = f.read()

    actions1 = parse_keymap(content1)
    actions2 = parse_keymap(content2)

    all_ids = sorted(set(actions1.keys()) | set(actions2.keys()))

    diffs = []

    for aid in all_ids:
        s1 = get_shortcuts(actions1.get(aid))
        s2 = get_shortcuts(actions2.get(aid))

        if aid not in actions1:
            diffs.append(f"[ADDED in Elior (2)] {aid}: {', '.join(s2)}")
        elif aid not in actions2:
            diffs.append(f"[REMOVED in Elior (2)] {aid}")
        elif s1 != s2:
            diffs.append(f"[CHANGED] {aid}:")
            diffs.append(f"  Mallowigi OSX: {', '.join(s1)}")
            diffs.append(f"  Elior (2):     {', '.join(s2)}")

    return diffs


if __name__ == "__main__":
    # Path to Elior (2).xml is passed as arg1, Mallowigi OSX.xml as arg2
    diffs = compare(sys.argv[2], sys.argv[1])
    for d in diffs:
        print(d)
