import re
import os

BASE = "/home/z/my-project/fleet-management/src/main/java/com/fleet/management/controller"

PAGEABLE_PARAM = '@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable'

def process_controller(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Check if already processed (has createPageable method)
    if 'createPageable' in content:
        print(f"  SKIP (already processed): {os.path.basename(filepath)}")
        return False

    # Check if has Pageable pageable
    if 'Pageable pageable' not in content:
        print(f"  SKIP (no Pageable): {os.path.basename(filepath)}")
        return False

    # 1. Replace @PageableDefault(...) Pageable pageable with 4 @RequestParam params
    content = content.replace(
        PAGEABLE_PARAM,
        '@RequestParam(defaultValue = "0") Integer page, '
        '@RequestParam(defaultValue = "20") Integer perPage, '
        '@RequestParam(defaultValue = "id") String sort, '
        '@RequestParam(defaultValue = "ASC") String sortOrder'
    )

    # 2. Remove import of PageableDefault
    content = content.replace('import org.springframework.data.web.PageableDefault;\n', '')

    # 3. Add import for PageRequest if not present
    if 'import org.springframework.data.domain.PageRequest;' not in content:
        content = content.replace(
            'import org.springframework.data.domain.Pageable;',
            'import org.springframework.data.domain.PageRequest;\nimport org.springframework.data.domain.Pageable;'
        )

    # 4. Find all methods that now have the 4 params and need pageable creation
    # Pattern: methods with (..., String sortOrder) {\n\n        return
    # We need to insert PageRequest creation before the return statement
    
    # Match method signatures ending with String sortOrder) followed by whitespace and body
    pattern = r'(String sortOrder\)\s*\{)\s*(\n\s*)(return )'
    
    def add_pageable_creation(match):
        return match.group(1) + match.group(2) + \
               'Pageable pageable = PageRequest.of(page, perPage, Sort.Direction.fromString(sortOrder), sort);\n' + \
               match.group(2) + match.group(3)
    
    content = re.sub(pattern, add_pageable_creation, content)

    # 5. Remove empty lines left by removed imports
    content = re.sub(r'\n\n\n', '\n\n', content)

    with open(filepath, 'w') as f:
        f.write(content)
    
    print(f"  OK: {os.path.basename(filepath)}")
    return True

for filename in sorted(os.listdir(BASE)):
    if filename.endswith('Controller.java'):
        filepath = os.path.join(BASE, filename)
        process_controller(filepath)
