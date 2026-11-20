-- Improve RLS policy evaluation and cover frequently-joined foreign keys.
-- Scalar subqueries allow auth.uid() to be evaluated once per statement.

drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own" on public.profiles
  for select to authenticated using ((select auth.uid()) = id);

drop policy if exists "profiles_insert_own" on public.profiles;
create policy "profiles_insert_own" on public.profiles
  for insert to authenticated with check ((select auth.uid()) = id);

drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own" on public.profiles
  for update to authenticated
  using ((select auth.uid()) = id)
  with check ((select auth.uid()) = id);

drop policy if exists "themes_read_public_or_own" on public.themes;
create policy "themes_read_public_or_own" on public.themes
  for select to anon, authenticated
  using (is_public = true or (select auth.uid()) = owner_id);

drop policy if exists "themes_insert_own" on public.themes;
create policy "themes_insert_own" on public.themes
  for insert to authenticated with check ((select auth.uid()) = owner_id);

drop policy if exists "themes_update_own" on public.themes;
create policy "themes_update_own" on public.themes
  for update to authenticated
  using ((select auth.uid()) = owner_id)
  with check ((select auth.uid()) = owner_id);

drop policy if exists "themes_delete_own" on public.themes;
create policy "themes_delete_own" on public.themes
  for delete to authenticated using ((select auth.uid()) = owner_id);

drop policy if exists "ratings_read" on public.theme_ratings;
create policy "ratings_read" on public.theme_ratings
  for select to anon, authenticated
  using (
    exists (
      select 1 from public.themes t
      where t.id = theme_id
        and (t.is_public = true or t.owner_id = (select auth.uid()))
    )
  );

drop policy if exists "ratings_insert_own" on public.theme_ratings;
create policy "ratings_insert_own" on public.theme_ratings
  for insert to authenticated
  with check (
    (select auth.uid()) = user_id
    and exists (
      select 1 from public.themes t
      where t.id = theme_id and t.is_public = true
    )
  );

drop policy if exists "ratings_delete_own" on public.theme_ratings;
create policy "ratings_delete_own" on public.theme_ratings
  for delete to authenticated using ((select auth.uid()) = user_id);

drop policy if exists "dictionary_backup_select_own" on public.dictionary_backups;
create policy "dictionary_backup_select_own" on public.dictionary_backups
  for select to authenticated using ((select auth.uid()) = user_id);

drop policy if exists "dictionary_backup_insert_own" on public.dictionary_backups;
create policy "dictionary_backup_insert_own" on public.dictionary_backups
  for insert to authenticated with check ((select auth.uid()) = user_id);

drop policy if exists "dictionary_backup_update_own" on public.dictionary_backups;
create policy "dictionary_backup_update_own" on public.dictionary_backups
  for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

drop policy if exists "dictionary_backup_delete_own" on public.dictionary_backups;
create policy "dictionary_backup_delete_own" on public.dictionary_backups
  for delete to authenticated using ((select auth.uid()) = user_id);

create index if not exists theme_download_events_user_id_idx
  on public.theme_download_events (user_id);
create index if not exists theme_favorites_theme_id_idx
  on public.theme_favorites (theme_id);
create index if not exists theme_ratings_user_id_idx
  on public.theme_ratings (user_id);
create index if not exists user_preferences_active_theme_id_idx
  on public.user_preferences (active_theme_id);
