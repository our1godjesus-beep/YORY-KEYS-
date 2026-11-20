-- Supabase pgTAP contract tests. Run after all repository migrations.
create extension if not exists pgtap with schema extensions;
begin;
select extensions.plan(24);

select extensions.ok(
  (select relrowsecurity from pg_class where oid = 'public.profiles'::regclass),
  'RLS enabled on profiles'
);
select extensions.ok(
  (select relrowsecurity from pg_class where oid = 'public.themes'::regclass),
  'RLS enabled on themes'
);
select extensions.ok(
  (select relrowsecurity from pg_class where oid = 'public.theme_ratings'::regclass),
  'RLS enabled on theme ratings'
);
select extensions.ok(
  (select relrowsecurity from pg_class where oid = 'public.dictionary_backups'::regclass),
  'RLS enabled on encrypted dictionary backups'
);
select extensions.ok(
  (select relrowsecurity from pg_class where oid = 'public.language_pack_manifest'::regclass),
  'RLS enabled on language pack manifest'
);
select extensions.ok(
  (select relrowsecurity from pg_class where oid = 'public.user_preferences'::regclass),
  'RLS enabled on user preferences'
);
select extensions.ok(
  (select relrowsecurity from pg_class where oid = 'public.theme_favorites'::regclass),
  'RLS enabled on theme favorites'
);
select extensions.ok(
  (select relrowsecurity from pg_class where oid = 'public.theme_download_events'::regclass),
  'RLS enabled on theme download events'
);
select extensions.ok(
  (select relrowsecurity from pg_class where oid = 'public.sticker_packs'::regclass),
  'RLS enabled on sticker packs'
);
select extensions.ok(
  (select relrowsecurity from pg_class where oid = 'storage.objects'::regclass),
  'RLS enabled on Storage objects'
);

select extensions.ok(
  not has_table_privilege('anon', 'public.profiles', 'select'),
  'Anonymous role cannot read user profiles'
);
select extensions.ok(
  not has_table_privilege('anon', 'public.dictionary_backups', 'select'),
  'Anonymous role cannot read encrypted dictionary backups'
);
select extensions.ok(
  not has_table_privilege('anon', 'public.user_preferences', 'select'),
  'Anonymous role cannot read user preferences'
);
select extensions.ok(
  has_table_privilege('anon', 'public.themes', 'select'),
  'Anonymous role can query themes through public-theme RLS'
);
select extensions.ok(
  not has_table_privilege('anon', 'public.themes', 'insert'),
  'Anonymous role cannot publish themes'
);
select extensions.ok(
  not has_table_privilege('anon', 'public.themes', 'update'),
  'Anonymous role cannot edit themes'
);
select extensions.ok(
  has_table_privilege('anon', 'public.language_pack_manifest', 'select'),
  'Language pack manifests are publicly readable'
);
select extensions.ok(
  not has_table_privilege('anon', 'public.language_pack_manifest', 'insert'),
  'Anonymous role cannot publish language packs'
);
select extensions.ok(
  has_table_privilege('authenticated', 'public.dictionary_backups', 'select'),
  'Authenticated users may access the backup table subject to owner RLS'
);
select extensions.ok(
  has_table_privilege('authenticated', 'public.dictionary_backups', 'insert'),
  'Authenticated users may create their own encrypted backup rows'
);
select extensions.ok(
  exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'themes' and policyname = 'themes_read_public_or_own'),
  'Theme reads are restricted by the public-or-owner policy'
);
select extensions.ok(
  exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'dictionary_backups' and policyname = 'dictionary_backup_select_own'),
  'Dictionary backup reads use an owner policy'
);
select extensions.ok(
  exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'sticker_packs' and policyname = 'sticker_packs_read_public_or_own'),
  'Sticker pack reads use a public-or-owner policy'
);
select extensions.ok(
  exists (select 1 from pg_policies where schemaname = 'storage' and tablename = 'objects' and policyname = 'yory_sticker_owner_select'),
  'Private sticker assets have an owner-scoped read policy'
);

select * from extensions.finish();
rollback;
