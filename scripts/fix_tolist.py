#!/usr/bin/env python3
import glob, re

BASE = '/home/z/my-project/fleet-management/src/main/java/com/fleet/management/service/impl'

for fpath in sorted(glob.glob(f'{BASE}/*ServiceImpl.java')):
    with open(fpath) as f:
        content = f.read()
    orig = content
    
    # Fix: repository.Xxx(arg).stream().map(this::toResponse).toList()  (may be multi-line)
    # Strategy: replace any occurrence of the pattern
    content = re.sub(
        r'repository\.findAll\(\)\.stream\(\)\s*\.map\(this::toResponse\)\s*\.toList\(\)\s*;',
        'repository.findAll(pageable).map(this::toResponse);',
        content
    )
    
    # Fix: repository.findXxx(params).stream().map(this::toResponse).toList()  (may be multi-line)
    content = re.sub(
        r'(repository\.\w+\([^)]*\))\.stream\(\)\s*\.map\(this::toResponse\)\s*\.toList\(\)\s*;',
        r'\1, pageable).map(this::toResponse);',
        content
    )
    
    if content != orig:
        with open(fpath, 'w') as f:
            f.write(content)
        print(f'OK: {fpath.split("/")[-1]}')
    else:
        print(f'SKIP: {fpath.split("/")[-1]}')
