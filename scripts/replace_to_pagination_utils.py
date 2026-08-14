import re
import os

BASE = "/home/z/my-project/fleet-management/src/main/java/com/fleet/management/controller"

OLD_LINE = 'Pageable pageable = PageRequest.of(page, perPage, Sort.Direction.fromString(sortOrder), sort);'
NEW_LINE = 'Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));'

for filename in sorted(os.listdir(BASE)):
    if not filename.endswith('Controller.java'):
        continue
    filepath = os.path.join(BASE, filename)
    with open(filepath, 'r') as f:
        content = f.read()

    if OLD_LINE not in content:
        print(f"  SKIP: {filename}")
        continue

    # 1. Replace the PageRequest line
    content = content.replace(OLD_LINE, NEW_LINE)

    # 2. Remove import PageRequest
    content = content.replace('import org.springframework.data.domain.PageRequest;\n', '')

    # 3. Remove import Sort
    content = content.replace('import org.springframework.data.domain.Sort;\n', '')

    # 4. Add import PaginationUtils (after the package line)
    content = content.replace(
        'import org.springframework.data.domain.Pageable;',
        'import com.fleet.management.util.PaginationUtils;\nimport org.springframework.data.domain.Pageable;'
    )

    # 5. Clean up triple newlines
    content = re.sub(r'\n\n\n', '\n\n', content)

    with open(filepath, 'w') as f:
        f.write(content)
    print(f"  OK: {filename}")
