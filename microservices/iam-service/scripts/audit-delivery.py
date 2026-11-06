#!/usr/bin/env python3
"""Read-only, dependency-free checks of the IAM delivery contract. Never prints secret values."""
from pathlib import Path
import hashlib,json,re,subprocess,sys
SERVICE=Path(__file__).resolve().parents[1]
ROOT=SERVICE.parents[1]
checks={}
def check(name,condition,detail):
    checks[name]={'passed':bool(condition),'detail':detail}
def walk(items):
    for item in items:
        if 'item'in item:yield from walk(item['item'])
        else:yield item
controllers=SERVICE/'iam-rest-api/src/main/java/istad/ha/iamservice/restapi/controller'
expected=set()
for source in controllers.glob('*.java'):
    text=source.read_text();match=re.search(r'@RequestMapping\("([^"]+)"\)',text);base=match.group(1)if match else ''
    expected.update((method.upper(),base+path)for method,path in re.findall(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\("([^"]*)"\))?',text))
collection=json.loads((ROOT/'postman/IAM-Assignment.postman_collection.json').read_text())
requests=list(walk(collection['item']));actual=set()
for item in requests:
    request=item['request'];path=request['url']['raw'].replace('{{iam_url}}','').split('?')[0]
    if not path.startswith('/api/'):continue
    for key,field in [('user_uuid','uuid'),('role_uuid','roleUuid'),('permission_uuid','permissionUuid'),('identity_uuid','identityUuid'),('my_identity_uuid','identityUuid')]:path=path.replace('{{'+key+'}}','{'+field+'}')
    if path.startswith('/api/roles/{roleUuid}'):path=path.replace('/api/roles/{roleUuid}','/api/roles/{uuid}',1)
    if path.startswith('/api/permissions/{permissionUuid}'):path=path.replace('/api/permissions/{permissionUuid}','/api/permissions/{uuid}',1)
    actual.add((request['method'],path))
check('rest_postman_coverage',len(expected)==39 and expected==actual,{'required':len(expected),'covered':len(expected&actual),'missing':sorted(expected-actual),'unexpected':sorted(actual-expected)})
required={
    'ActivatePermissionUseCase',
    'ActivateRoleUseCase',
    'AssignPermissionsToRoleUseCase',
    'AssignRolesToUserUseCase',
    'ChangePasswordUseCase',
    'CreatePermissionUseCase',
    'CreateRoleUseCase',
    'CreateUserUseCase',
    'DeactivatePermissionUseCase',
    'DeactivateRoleUseCase',
    'DeletePermissionUseCase',
    'DeleteRoleUseCase',
    'DeleteUserUseCase',
    'DisableUserUseCase',
    'EnableUserUseCase',
    'FindOrProvisionFederatedUserUseCase',
    'GetPermissionUseCase',
    'GetRoleUseCase',
    'GetUserAuthoritiesUseCase',
    'GetUserUseCase',
    'GrantPermissionsToUserUseCase',
    'LinkIdentityUseCase',
    'ListPermissionsUseCase',
    'ListRolesUseCase',
    'ListUserIdentitiesUseCase',
    'ListUsersUseCase',
    'LoadUserForAuthenticationUseCase',
    'LockUserUseCase',
    'RecordLoginAttemptUseCase',
    'RegisterUserUseCase',
    'ResetUserPasswordUseCase',
    'RevokePermissionFromRoleUseCase',
    'RevokePermissionFromUserUseCase',
    'RevokeRoleFromUserUseCase',
    'UnlinkIdentityUseCase',
    'UnlockUserUseCase',
    'UpdatePermissionUseCase',
    'UpdateRoleUseCase',
    'UpdateUserProfileUseCase',
}
implemented={p.stem for p in SERVICE.glob('iam-domain/iam-application-service/src/main/java/**/usecase/*UseCase.java')}
check('section_c_use_cases',len(required)==39 and required<=implemented,{'required':len(required),'implemented':len(implemented),'missing':sorted(required-implemented)})
environment=json.loads((ROOT/'postman/IAM-Assignment.postman_environment.json').read_text())
filled=[v['key']for v in environment['values']if any(term in v['key']for term in ['password','secret','token'])and v['value']]
check('postman_exports_no_secrets',not filled,{'nonemptySecretKeys':filled})
known={v['key']for v in environment['values']}|{'registered_user_uuid'}
refs=set(re.findall(r'\{\{([a-z_]+)\}\}',json.dumps(collection)))
check('postman_variable_contract',refs<=known,{'undefined':sorted(refs-known)})
text=(SERVICE/'iam-main/src/main/resources/application.yaml').read_text()
literal=[]
for number,line in enumerate(text.splitlines(),1):
    if re.match(r'\s*(client-secret|password|.*-password|.*-client-secret):',line)and'${'not in line:literal.append(number)
java_literals=[]
for source in SERVICE.glob('**/src/main/java/**/*.java'):
    if re.search(r'\.clientSecret\("|encoder\.encode\("',source.read_text()):java_literals.append(str(source.relative_to(SERVICE)))
check('external_runtime_secrets',not literal and not java_literals,{'literalSecretConfigLines':literal,'literalJavaCredentialFiles':java_literals})
keys=[]
for name in ['messages.properties','messages_km.properties']:
    keys.append({line.split('=',1)[0]for line in (SERVICE/'iam-web/src/main/resources'/name).read_text().splitlines()if'='in line and not line.startswith('#')})
check('locale_bundle_parity',keys[0]==keys[1],{'english':len(keys[0]),'khmer':len(keys[1])})
pages=['login','register','consent','index','profile-edit','change-password','linked-accounts','connected-apps']
check('section_f_templates',all((SERVICE/'iam-web/src/main/jte'/f'{page}.jte').is_file()for page in pages),{'templates':pages})
framework=[]
for module in ['iam-domain-core','iam-application-service']:
    for source in (SERVICE/'iam-domain'/module/'src/main/java').rglob('*.java'):
        if re.search(r'(?m)^import\s+(org\.springframework|jakarta\.persistence|jakarta\.servlet|org\.hibernate|gg\.jte)\.',source.read_text()):framework.append(str(source.relative_to(SERVICE)))
check('framework_free_inner_layers',not framework,{'violations':framework})
changed=[]
for source in sorted((SERVICE/'iam-persistence/src/main/resources/db/changelog/ddl').glob('00[1-5]*')):
    relative=str(source.relative_to(ROOT));baseline=subprocess.run(['git','show','HEAD:'+relative],cwd=ROOT,capture_output=True)
    if baseline.returncode or hashlib.sha256(baseline.stdout).digest()!=hashlib.sha256(source.read_bytes()).digest():changed.append(source.name)
check('legacy_migrations_preserved',not changed,{'changed':changed})
result={'checks':checks,'passed':all(c['passed']for c in checks.values()),'note':'Static contract evidence only; execute Gradle, database, Postman and browser checks separately. Live provider-account acceptance is manual.'}
print(json.dumps(result,indent=2));sys.exit(0 if result['passed']else 1)
