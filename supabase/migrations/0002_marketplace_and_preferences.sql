-- YORY KEYS marketplace and preferences.
-- No keystrokes, prediction context, clipboard payloads, screenshots or plaintext
-- personal dictionary entries belong in this migration.

create table if not exists public.user_preferences (
  user_id uuid primary key references auth.users(id) on delete cascade,
  active_theme_id uuid references public.themes(id) on delete set null,
  appearance jsonb not null default '{}'::jsonb check (jsonb_typeof(appearance) = 'object'),
  sound_enabled boolean not null default true,
  haptics_enabled boolean not null default true,
  active_languages text[] not null default array['en-US']::text[],
  updated_at timestamptz not null default now(),
  check (cardinality(active_languages) between 1 and 5)
);

create table if not exists public.theme_favorites (
  user_id uuid not null references auth.users(id) on delete cascade,
  theme_id uuid not null references public.themes(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, theme_id)
);

create table if not exists public.theme_download_events (
  id uuid primary key default gen_random_uuid(),
  theme_id uuid not null references public.themes(id) on delete cascade,
  user_id uuid references auth.users(id) on delete set null,
  downloaded_at timestamptz not null default now()
);

create index if not exists theme_download_events_theme_date_idx
  on public.theme_download_events (theme_id, downloaded_at desc);

create table if not exists public.sticker_packs (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null check (char_length(name) between 1 and 48),
  manifest_json jsonb not null check (jsonb_typeof(manifest_json) = 'object'),
  preview_path text,
  is_public boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists sticker_packs_public_created_idx
  on public.sticker_packs (created_at desc) where is_public = true;
create index if not exists sticker_packs_owner_updated_idx
  on public.sticker_packs (owner_id, updated_at desc);

alter table public.user_preferences enable row level security;
alter table public.theme_favorites enable row level security;
alter table public.theme_download_events enable row level security;
alter table public.sticker_packs enable row level security;

drop policy if exists "user_preferences_select_own" on public.user_preferences;
create policy "user_preferences_select_own" on public.user_preferences
  for select to authenticated using ((select auth.uid()) = user_id);
drop policy if exists "user_preferences_insert_own" on public.user_preferences;
create policy "user_preferences_insert_own" on public.user_preferences
  for insert to authenticated with check ((select auth.uid()) = user_id);
drop policy if exists "user_preferences_update_own" on public.user_preferences;
create policy "user_preferences_update_own" on public.user_preferences
  for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
drop policy if exists "user_preferences_delete_own" on public.user_preferences;
create policy "user_preferences_delete_own" on public.user_preferences
  for delete to authenticated using ((select auth.uid()) = user_id);

drop policy if exists "theme_favorites_select_own" on public.theme_favorites;
create policy "theme_favorites_select_own" on public.theme_favorites
  for select to authenticated using ((select auth.uid()) = user_id);
drop policy if exists "theme_favorites_insert_own_public_theme" on public.theme_favorites;
create policy "theme_favorites_insert_own_public_theme" on public.theme_favorites
  for insert to authenticated
  with check (
    (select auth.uid()) = user_id
    and exists (
      select 1 from public.themes t
      where t.id = theme_id and t.is_public = true
    )
  );
drop policy if exists "theme_favorites_delete_own" on public.theme_favorites;
create policy "theme_favorites_delete_own" on public.theme_favorites
  for delete to authenticated using ((select auth.uid()) = user_id);

drop policy if exists "theme_download_events_insert_authenticated" on public.theme_download_events;
create policy "theme_download_events_insert_authenticated" on public.theme_download_events
  for insert to authenticated
  with check (
    (select auth.uid()) = user_id
    and exists (
      select 1 from public.themes t
      where t.id = theme_id and t.is_public = true
    )
  );
drop policy if exists "theme_download_events_select_own" on public.theme_download_events;
create policy "theme_download_events_select_own" on public.theme_download_events
  for select to authenticated using ((select auth.uid()) = user_id);

drop policy if exists "sticker_packs_read_public_or_own" on public.sticker_packs;
create policy "sticker_packs_read_public_or_own" on public.sticker_packs
  for select to anon, authenticated
  using (is_public = true or (select auth.uid()) = owner_id);
drop policy if exists "sticker_packs_insert_own" on public.sticker_packs;
create policy "sticker_packs_insert_own" on public.sticker_packs
  for insert to authenticated with check ((select auth.uid()) = owner_id);
drop policy if exists "sticker_packs_update_own" on public.sticker_packs;
create policy "sticker_packs_update_own" on public.sticker_packs
  for update to authenticated
  using ((select auth.uid()) = owner_id)
  with check ((select auth.uid()) = owner_id);
drop policy if exists "sticker_packs_delete_own" on public.sticker_packs;
create policy "sticker_packs_delete_own" on public.sticker_packs
  for delete to authenticated using ((select auth.uid()) = owner_id);

-- Recreate rating update policy with a public-theme check in both clauses.
drop policy if exists "ratings_update_own" on public.theme_ratings;
create policy "ratings_update_own" on public.theme_ratings
  for update to authenticated
  using (
    (select auth.uid()) = user_id
    and exists (select 1 from public.themes t where t.id = theme_id and t.is_public = true)
  )
  with check (
    (select auth.uid()) = user_id
    and exists (select 1 from public.themes t where t.id = theme_id and t.is_public = true)
  );

-- Storage buckets: public preview assets and private sticker assets.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values
  ('yory-theme-previews', 'yory-theme-previews', true, 5242880, array['image/jpeg','image/png','image/webp']),
  ('yory-sticker-assets', 'yory-sticker-assets', false, 15728640, array['image/png','image/webp'])
on conflict (id) do nothing;

drop policy if exists "yory_theme_preview_public_read" on storage.objects;
create policy "yory_theme_preview_public_read" on storage.objects
  for select to anon, authenticated
  using (bucket_id = 'yory-theme-previews');

drop policy if exists "yory_theme_preview_owner_insert" on storage.objects;
create policy "yory_theme_preview_owner_insert" on storage.objects
  for insert to authenticated
  with check (
    bucket_id = 'yory-theme-previews'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );
drop policy if exists "yory_theme_preview_owner_update" on storage.objects;
create policy "yory_theme_preview_owner_update" on storage.objects
  for update to authenticated
  using (
    bucket_id = 'yory-theme-previews'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  )
  with check (
    bucket_id = 'yory-theme-previews'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );
drop policy if exists "yory_theme_preview_owner_delete" on storage.objects;
create policy "yory_theme_preview_owner_delete" on storage.objects
  for delete to authenticated
  using (
    bucket_id = 'yory-theme-previews'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );

drop policy if exists "yory_sticker_owner_select" on storage.objects;
create policy "yory_sticker_owner_select" on storage.objects
  for select to authenticated
  using (
    bucket_id = 'yory-sticker-assets'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );
drop policy if exists "yory_sticker_owner_insert" on storage.objects;
create policy "yory_sticker_owner_insert" on storage.objects
  for insert to authenticated
  with check (
    bucket_id = 'yory-sticker-assets'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );
drop policy if exists "yory_sticker_owner_update" on storage.objects;
create policy "yory_sticker_owner_update" on storage.objects
  for update to authenticated
  using (
    bucket_id = 'yory-sticker-assets'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  )
  with check (
    bucket_id = 'yory-sticker-assets'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );
drop policy if exists "yory_sticker_owner_delete" on storage.objects;
create policy "yory_sticker_owner_delete" on storage.objects
  for delete to authenticated
  using (
    bucket_id = 'yory-sticker-assets'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );

comment on table public.user_preferences is
  'Non-content preferences only; never store keystrokes, clipboard contents or prediction context.';
comment on table public.dictionary_backups is
  'Ciphertext-only client-encrypted dictionary backups. Never insert plaintext words.';
