-- YORY KEYS cloud foundation.
-- Never store keystrokes, live prediction context, or clipboard contents here.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text check (char_length(display_name) <= 60),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.themes (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null check (char_length(name) between 1 and 48),
  schema_version integer not null default 1 check (schema_version = 1),
  theme_json jsonb not null,
  is_public boolean not null default false,
  preview_path text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists themes_public_created_idx
  on public.themes (created_at desc) where is_public = true;
create index if not exists themes_owner_updated_idx
  on public.themes (owner_id, updated_at desc);

create table if not exists public.theme_ratings (
  theme_id uuid not null references public.themes(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  rating smallint not null check (rating between 1 and 5),
  created_at timestamptz not null default now(),
  primary key (theme_id, user_id)
);

-- Client-side encryption is mandatory: ciphertext only.
create table if not exists public.dictionary_backups (
  user_id uuid not null references auth.users(id) on delete cascade,
  device_id uuid not null,
  ciphertext bytea not null,
  nonce bytea not null,
  key_version integer not null default 1,
  updated_at timestamptz not null default now(),
  primary key (user_id, device_id),
  check (octet_length(ciphertext) <= 1048576),
  check (octet_length(nonce) between 12 and 32)
);

create table if not exists public.language_pack_manifest (
  language_tag text not null,
  version integer not null,
  storage_path text not null,
  sha256 text not null check (sha256 ~ '^[a-f0-9]{64}$'),
  signature text not null,
  minimum_app_version text not null,
  published_at timestamptz not null default now(),
  primary key (language_tag, version)
);

alter table public.profiles enable row level security;
alter table public.themes enable row level security;
alter table public.theme_ratings enable row level security;
alter table public.dictionary_backups enable row level security;
alter table public.language_pack_manifest enable row level security;

create policy "profiles_select_own" on public.profiles
  for select to authenticated using (auth.uid() = id);
create policy "profiles_insert_own" on public.profiles
  for insert to authenticated with check (auth.uid() = id);
create policy "profiles_update_own" on public.profiles
  for update to authenticated using (auth.uid() = id) with check (auth.uid() = id);

create policy "themes_read_public_or_own" on public.themes
  for select to anon, authenticated using (is_public = true or auth.uid() = owner_id);
create policy "themes_insert_own" on public.themes
  for insert to authenticated with check (auth.uid() = owner_id);
create policy "themes_update_own" on public.themes
  for update to authenticated using (auth.uid() = owner_id) with check (auth.uid() = owner_id);
create policy "themes_delete_own" on public.themes
  for delete to authenticated using (auth.uid() = owner_id);

create policy "ratings_read" on public.theme_ratings
  for select to anon, authenticated using (
    exists (select 1 from public.themes t where t.id = theme_id and (t.is_public = true or t.owner_id = auth.uid()))
  );
create policy "ratings_insert_own" on public.theme_ratings
  for insert to authenticated with check (
    auth.uid() = user_id and exists (select 1 from public.themes t where t.id = theme_id and t.is_public = true)
  );
create policy "ratings_update_own" on public.theme_ratings
  for update to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "ratings_delete_own" on public.theme_ratings
  for delete to authenticated using (auth.uid() = user_id);

create policy "dictionary_backup_select_own" on public.dictionary_backups
  for select to authenticated using (auth.uid() = user_id);
create policy "dictionary_backup_insert_own" on public.dictionary_backups
  for insert to authenticated with check (auth.uid() = user_id);
create policy "dictionary_backup_update_own" on public.dictionary_backups
  for update to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "dictionary_backup_delete_own" on public.dictionary_backups
  for delete to authenticated using (auth.uid() = user_id);

create policy "language_manifest_read" on public.language_pack_manifest
  for select to anon, authenticated using (true);

-- Storage buckets and Edge Functions should be configured separately.
-- Do not place Supabase service-role keys in mobile apps.
