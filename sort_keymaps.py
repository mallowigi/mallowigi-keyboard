import sys
import re
import os

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
    
    actions = []
    matches = list(action_pattern.finditer(content))
    last_end = 0
    for i, match in enumerate(matches):
        if match.start() < last_end:
            continue
            
        full_tag_start = match.group(1)
        action_id = match.group(2)
        attributes_and_closing = match.group(3)
        
        if attributes_and_closing.strip().endswith('/'):
            # Self-closing
            actions.append((action_id, full_tag_start))
            last_end = match.end()
        else:
            # Has a closing tag </action>
            end_tag = "</action>"
            start_pos = match.end()
            end_pos = content.find(end_tag, start_pos)
            if end_pos != -1:
                full_tag = content[match.start():end_pos + len(end_tag)]
                actions.append((action_id, full_tag))
                last_end = end_pos + len(end_tag)
            else:
                # Fallback if closing tag is missing (should not happen in valid XML)
                actions.append((action_id, full_tag_start))
                last_end = match.end()
    
    if not actions:
        print(f"No actions found in: {file_path}")
        return
    
    # Sort actions by id (case-insensitive to match common expectations)
    actions.sort(key=lambda x: x[0].lower())
    
    sorted_actions_content = "".join([a[1] for a in actions])
    
    # Construct the new content
    new_content = f"{root_start_tag}{sorted_actions_content}\n</keymap>\n"
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print(f"Sorted: {file_path}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python sort_keymaps.py <file1.xml> <file2.xml> ...")
        sys.exit(1)
        
    for arg in sys.argv[1:]:
        sort_keymap(arg)
