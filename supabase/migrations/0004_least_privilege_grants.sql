-- YORY KEYS: least-privilege API grants.
-- Run after 0001-0003 in a local Supabase stack first.
-- This file is committed only; it has not been applied to a hosted database.

revoke all on table
  public.profiles,
  public.themes,
  public.theme_ratings,
  public.dictionary_backups,
  public.language_pack_manifest,
  public.user_preferences,
  public.theme_favorites,
  public.theme_download_events,
  public.sticker_packs
from anon, authenticated;

grant usage on schema public to anon, authenticated;

-- Public theme discovery is governed by RLS; private themes remain owner-only.
grant select on public.themes to anon, authenticated;
grant insert, update, delete on public.themes to authenticated;

grant select on public.theme_ratings to anon, authenticated;
grant insert, update, delete on public.theme_ratings to authenticated;

grant select, insert, update, delete on public.profiles to authenticated;
grant select, insert, update, delete on public.dictionary_backups to authenticated;
grant select on public.language_pack_manifest to anon, authenticated;

grant select, insert, update, delete on public.user_preferences to authenticated;
grant select, insert, delete on public.theme_favorites to authenticated;
grant select, insert on public.theme_download_events to authenticated;

grant select on public.sticker_packs to anon, authenticated;
grant insert, update, delete on public.sticker_packs to authenticated;

-- Row-level Storage policies still determine which object paths can be accessed.
revoke all on table storage.objects from anon, authenticated;
grant select on table storage.objects to anon, authenticated;
grant insert, update, delete on table storage.objects to authenticated;

-- New public-schema tables must not silently inherit client privileges.
alter default privileges in schema public
  revoke all on tables from anon, authenticated;

-- Trigger-only routines are not part of the client API.
revoke all on function public.yory_set_updated_at() from public, anon, authenticated;
revoke all on function public.yory_handle_new_user() from public, anon, authenticated;
