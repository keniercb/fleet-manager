#!/usr/bin/env python3
import re, os

BASE = '/home/z/my-project/fleet-management/src/main/java/com/fleet/management'
PAGEABLE_IMPORTS = [
    'import org.springframework.data.domain.Page;',
    'import org.springframework.data.domain.Pageable;',
]
CTRL_IMPORTS = PAGEABLE_IMPORTS + [
    'import org.springframework.data.web.PageableDefault;',
    'import org.springframework.data.domain.Sort;',
]
PAGEABLE_PARAM = '@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable'

def read(f): return open(f).read()
def write(f, c):
    with open(f, 'w') as fh: fh.write(c)

def add_imports(filepath, imports):
    c = read(filepath)
    pkg_end = c.index(';') + 1
    before = c[:pkg_end]
    after = c[pkg_end:]
    for imp in imports:
        if imp not in c:
            after = '\n' + imp + after
    write(filepath, before + after)

def remove_imports(filepath, imports):
    c = read(filepath)
    for imp in imports:
        c = c.replace(imp + '\n', '')
    write(filepath, c)

# ===================== REPOSITORIES =====================
print('=== REPOSITORIES ===')

# VehiculoRepository
f = f'{BASE}/repository/VehiculoRepository.java'
c = read(f)
c = c.replace('List<Vehiculo> findByChoferId(Long choferId)', 'Page<Vehiculo> findByChoferId(Long choferId, Pageable pageable)')
c = c.replace('List<Vehiculo> findByTipoVehiculoId(Long tipoVehiculoId)', 'Page<Vehiculo> findByTipoVehiculoId(Long tipoVehiculoId, Pageable pageable)')
c = c.replace('List<Vehiculo> findByTipoCombustibleId(Long tipoCombustibleId)', 'Page<Vehiculo> findByTipoCombustibleId(Long tipoCombustibleId, Pageable pageable)')
c = c.replace('List<Vehiculo> findSinChoferAsignado()', 'Page<Vehiculo> findSinChoferAsignado(Pageable pageable)')
c = c.replace('List<Vehiculo> findActivosByChoferId(@Param("choferId") Long choferId)', 'Page<Vehiculo> findActivosByChoferId(@Param("choferId") Long choferId, Pageable pageable)')
write(f, c)
add_imports(f, PAGEABLE_IMPORTS)
print('  OK: VehiculoRepository')

# ChoferCategoriaRepository
f = f'{BASE}/repository/ChoferCategoriaRepository.java'
c = read(f)
c = c.replace('List<ChoferCategoria> findByChoferId(Long choferId)', 'Page<ChoferCategoria> findByChoferId(Long choferId, Pageable pageable)')
c = c.replace('List<ChoferCategoria> findByCategoriaLicenciaId(Long categoriaLicenciaId)', 'Page<ChoferCategoria> findByCategoriaLicenciaId(Long categoriaLicenciaId, Pageable pageable)')
c = c.replace('List<ChoferCategoria> findActivosByChoferId(@Param("choferId") Long choferId)', 'Page<ChoferCategoria> findActivosByChoferId(@Param("choferId") Long choferId, Pageable pageable)')
c = c.replace('List<ChoferCategoria> findActivosByCategoriaLicenciaId(@Param("categoriaId") Long categoriaId)', 'Page<ChoferCategoria> findActivosByCategoriaLicenciaId(@Param("categoriaId") Long categoriaId, Pageable pageable)')
write(f, c)
add_imports(f, PAGEABLE_IMPORTS)
print('  OK: ChoferCategoriaRepository')

# RecorridoRepository
f = f'{BASE}/repository/RecorridoRepository.java'
c = read(f)
c = c.replace('List<Recorrido> findByVehiculoId(Long vehiculoId)', 'Page<Recorrido> findByVehiculoId(Long vehiculoId, Pageable pageable)')
c = c.replace(
    'List<Recorrido> findByVehiculoIdAndFechaBetween(@Param("vehiculoId") Long vehiculoId,\n                                                    @Param("desde") LocalDate desde,\n                                                    @Param("hasta") LocalDate hasta)',
    'Page<Recorrido> findByVehiculoIdAndFechaBetween(@Param("vehiculoId") Long vehiculoId,\n                                                    @Param("desde") LocalDate desde,\n                                                    @Param("hasta") LocalDate hasta,\n                                                    Pageable pageable)'
)
write(f, c)
add_imports(f, PAGEABLE_IMPORTS)
print('  OK: RecorridoRepository')

# ===================== SERVICE INTERFACES =====================
print('\n=== SERVICE INTERFACES ===')
import glob
for fpath in sorted(glob.glob(f'{BASE}/service/*Service.java')):
    if 'Impl' in fpath: continue
    c = read(fpath)
    orig = c
    # findAll() -> findAll(Pageable pageable)
    c = re.sub(r'List<(\w+Response)> (findAll)\(\)', r'Page<\1> \2(Pageable pageable)', c)
    # findByXxx(param) -> findByXxx(param, Pageable pageable)
    c = re.sub(r'List<(\w+Response)> (find\w+)\(([^)]+)\)', r'Page<\1> \2(\3, Pageable pageable)', c)
    if c != orig:
        write(fpath, c)
        add_imports(fpath, PAGEABLE_IMPORTS)
        print(f'  OK: {os.path.basename(fpath)}')
    else:
        print(f'  SKIP: {os.path.basename(fpath)}')

# ===================== SERVICE IMPLEMENTATIONS =====================
print('\n=== SERVICE IMPLEMENTATIONS ===')
for fpath in sorted(glob.glob(f'{BASE}/service/impl/*ServiceImpl.java')):
    c = read(fpath)
    orig = c
    
    # 1. Replace method signatures
    c = re.sub(r'public List<(\w+Response)> (findAll)\(\)', r'public Page<\1> \2(Pageable pageable)', c)
    c = re.sub(r'public List<(\w+Response)> (find\w+)\(([^)]+)\)', r'public Page<\1> \2(\3, Pageable pageable)', c)
    
    # 2. Replace method bodies: .findAll().stream().map(this::toResponse).toList() -> .findAll(pageable).map(this::toResponse)
    c = c.replace('repository.findAll().stream().map(this::toResponse).toList()', 'repository.findAll(pageable).map(this::toResponse)')
    
    # 3. Replace: repository.findXxx(arg).stream().map(this::toResponse).toList() -> repository.findXxx(arg, pageable).map(this::toResponse)
    c = re.sub(
        r'repository\.(\w+)\(([^)]+)\)\.stream\(\)\.map\(this::toResponse\)\.toList\(\)',
        r'repository.\1(\2, pageable).map(this::toResponse)',
        c
    )
    
    if c != orig:
        write(fpath, c)
        add_imports(fpath, PAGEABLE_IMPORTS)
        print(f'  OK: {os.path.basename(fpath)}')
    else:
        print(f'  SKIP: {os.path.basename(fpath)}')

# ===================== CONTROLLERS =====================
print('\n=== CONTROLLERS ===')
PAGEABLE_ANNOT = '@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC)'

for fpath in sorted(glob.glob(f'{BASE}/controller/*Controller.java')):
    c = read(fpath)
    orig = c
    
    # 1. Replace ResponseEntity<List<...>> with ResponseEntity<Page<...>>
    c = c.replace('ResponseEntity<List<', 'ResponseEntity<Page<')
    
    # 2. Replace service.findAll() with service.findAll(pageable)
    c = c.replace('service.findAll()', 'service.findAll(pageable)')
    
    # 3. Replace service.findByXxx(param) with service.findByXxx(param, pageable) for list methods
    # Only match calls that don't already have pageable
    c = re.sub(
        r'service\.(find\w+)\(([^)]+)\)(?!.*pageable)',
        r'service.\1(\2, pageable)',
        c
    )
    
    # 4. Add Pageable param to list method signatures
    # Pattern: public ResponseEntity<Page<...>> findXxx(@PathVariable Long id) {
    # -> public ResponseEntity<Page<...>> findXxx(@PathVariable Long id, @PageableDefault(...) Pageable pageable) {
    # But NOT for findById, create, update, delete
    
    lines = c.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        # Detect list method signatures (have ResponseEntity<Page<)
        if 'ResponseEntity<Page<' in line and 'public' in line and '@PageableDefault' not in line:
            # Collect the full signature until we find the opening brace
            sig_lines = [line]
            j = i + 1
            while j < len(lines) and '{' not in lines[j]:
                sig_lines.append(lines[j])
                j += 1
            sig = ' '.join(l.strip() for l in sig_lines)
            
            if 'Pageable' not in sig:
                # Add pageable before the closing paren
                # Find the ) that precedes {
                paren_idx = sig.rfind(')')
                sig = sig[:paren_idx] + ', ' + PAGEABLE_ANNOT + ' Pageable pageable' + sig[paren_idx:]
                new_lines.append(sig + '\n')
            else:
                for sl in sig_lines:
                    new_lines.append(sl)
            i = j
        else:
            new_lines.append(line)
        i += 1
    
    c = '\n'.join(new_lines)
    
    # Remove unused List import
    c = c.replace('import java.util.List;\n', '')
    
    if c != orig:
        write(fpath, c)
        add_imports(fpath, CTRL_IMPORTS)
        print(f'  OK: {os.path.basename(fpath)}')
    else:
        print(f'  SKIP: {os.path.basename(fpath)}')

print('\nDone!')
