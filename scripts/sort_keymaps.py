import os
import re
import sys


def sort_keymap(file_path):
    if not os.path.exists(file_path):
        print(f"File not found: {file_path}")
        return

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find the root tag and its attributes
    root_match = re.search(r'(<keymap[^>]*>)', content)
    if not root_match:
        print(f"Not a valid keymap file: {file_path}")
        return
    
    root_start_tag = root_match.group(1)
    
    # Extract all action tags
    # Handles both self-closing <action id="..."/> and <action id="...">...</action>
    action_pattern = re.compile(r'(\s*<action id="([^"]+)"(.*?)>)', re.DOTALL)

    actions_map = {}  # action_id -> set of shortcut lines
    matches = list(action_pattern.finditer(content))
    last_end = 0
    for i, match in enumerate(matches):
        if match.start() < last_end:
            continue
            
        action_id = match.group(2)
        attributes_and_closing = match.group(3)
        
        if attributes_and_closing.strip().endswith('/'):
            # Self-closing
            if action_id not in actions_map:
                actions_map[action_id] = set()
            last_end = match.end()
        else:
            # Has a closing tag </action>
            end_tag = "</action>"
            start_pos = match.end()
            end_pos = content.find(end_tag, start_pos)
            if end_pos != -1:
                inner_content = content[match.end():end_pos].strip()
                lines = [line.strip() for line in inner_content.split('\n') if line.strip()]
                if action_id not in actions_map:
                    actions_map[action_id] = set()
                actions_map[action_id].update(lines)
                last_end = end_pos + len(end_tag)
            else:
                # Fallback
                if action_id not in actions_map:
                    actions_map[action_id] = set()
                last_end = match.end()

    if not actions_map:
        print(f"No actions found in: {file_path}")
        return

    # Sort actions by id
    sorted_ids = sorted(actions_map.keys(), key=lambda x: x.lower())

    output = []
    for aid in sorted_ids:
        shortcuts = actions_map[aid]
        if not shortcuts:
            output.append(f'    <action id="{aid}"/>')
        else:
            output.append(f'    <action id="{aid}">')
            for s in sorted(list(shortcuts)):
                output.append(f'        {s}')
            output.append(f'    </action>')

    sorted_actions_content = "\n".join(output)
    
    # Construct the new content
    new_content = f"{root_start_tag}\n{sorted_actions_content}\n</keymap>\n"
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print(f"Sorted: {file_path}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python sort_keymaps.py <file1.xml> <file2.xml> ...")
        sys.exit(1)
        
    for arg in sys.argv[1:]:
        sort_keymap(arg)
