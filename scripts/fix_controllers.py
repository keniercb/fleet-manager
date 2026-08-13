#!/usr/bin/env python3
import re, glob, os

BASE = '/home/z/my-project/fleet-management/src/main/java/com/fleet/management/controller'
PAGE_IMPORTS = [
    'import org.springframework.data.domain.Page;',
    'import org.springframework.data.domain.Pageable;',
    'import org.springframework.data.web.PageableDefault;',
    'import org.springframework.data.domain.Sort;',
]

def read(f): return open(f).read()
def write(f, c): open(f, 'w').write(c)

def add_imports(filepath, imports):
    c = read(filepath)
    pkg_end = c.index(';') + 1
    before = c[:pkg_end]
    after = c[pkg_end:]
    for imp in imports:
        if imp not in c:
            after = imp + '\n' + after
    write(filepath, before + '\n' + after)

PAGEABLE_DECL = '@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable'

def process_controller(filepath):
    c = read(filepath)
    orig = c
    
    # 1. ResponseEntity<List<...>> -> ResponseEntity<Page<...>>
    c = c.replace('ResponseEntity<List<', 'ResponseEntity<Page<')
    
    # 2. service.findAll() -> service.findAll(pageable)
    c = c.replace('service.findAll()', 'service.findAll(pageable)')
    
    # 3. Process line by line
    lines = c.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        
        # Check if this is a list method signature line (has ResponseEntity<Page< and public)
        if 'ResponseEntity<Page<' in line and 'public' in line and '@PageableDefault' not in line:
            # Collect full sig until {
            sig = line
            j = i + 1
            while '{' not in sig and j < len(lines):
                sig += ' ' + lines[j].strip()
                j += 1
            
            if 'Pageable' not in sig:
                paren = sig.rfind(')')
                before_paren = sig[:paren].rstrip()
                # Check if there are existing params (not just "()")
                if before_paren.endswith('('):
                    # No params: findAll() -> findAll(@PageableDefault... Pageable pageable)
                    sig = before_paren + PAGEABLE_DECL + sig[paren:]
                else:
                    # Has params: findXxx(@PathVariable Long id) -> findXxx(@PathVariable Long id, @PageableDefault... Pageable pageable)
                    sig = before_paren + ', ' + PAGEABLE_DECL + sig[paren:]
                
                # Write modified sig
                new_lines.append(sig + '\n')
                i = j
                continue
        
        # Check if this line has a service.findXxx(param) call that needs pageable
        if stripped.startswith('return ResponseEntity.ok(service.') and 'pageable' not in stripped:
            m = re.search(r'service\.(find\w+)\(([^)]*)\)', stripped)
            if m:
                method_name = m.group(1)
                if method_name not in ('findById', 'create', 'update', 'delete'):
                    old_call = f'service.{method_name}({m.group(2)})'
                    new_call = f'service.{method_name}({m.group(2)}, pageable)'
                    line = line.replace(old_call, new_call)
        
        new_lines.append(line)
        i += 1
    
    c = '\n'.join(new_lines)
    
    # Remove unused java.util.List import
    c = c.replace('import java.util.List;\n', '')
    
    if c != orig:
        write(filepath, c)
        add_imports(filepath, PAGE_IMPORTS)
        return True
    return False

print('=== CONTROLLERS ===')
for fpath in sorted(glob.glob(f'{BASE}/*Controller.java')):
    if process_controller(fpath):
        print(f'  OK: {os.path.basename(fpath)}')
    else:
        print(f'  SKIP: {os.path.basename(fpath)}')

print('Done!')
