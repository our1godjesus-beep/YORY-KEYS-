-- YORY KEYS schema hardening and account lifecycle.
-- This migration adds database-level validation and explicit grants without
-- changing any hosted project until the migration is deliberately applied.

alter table public.themes
  drop constraint if exists themes_theme_json_shape;

alter table public.themes
  add constraint themes_theme_json_shape check (
    jsonb_typeof(theme_json) = 'object'
    and theme_json ->> 'schemaVersion' = '1'
    and jsonb_typeof(theme_json -> 'appearance') = 'object'
    and (theme_json -> 'appearance') ?& array[
      'background', 'key', 'keyPressed', 'text', 'accent'
    ]
  );

create or replace function public.yory_set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists yory_profiles_updated_at on public.profiles;
create trigger yory_profiles_updated_at
  before update on public.profiles
  for each row execute function public.yory_set_updated_at();

drop trigger if exists yory_themes_updated_at on public.themes;
create trigger yory_themes_updated_at
  before update on public.themes
  for each row execute function public.yory_set_updated_at();

drop trigger if exists yory_preferences_updated_at on public.user_preferences;
create trigger yory_preferences_updated_at
  before update on public.user_preferences
  for each row execute function public.yory_set_updated_at();

drop trigger if exists yory_sticker_packs_updated_at on public.sticker_packs;
create trigger yory_sticker_packs_updated_at
  before update on public.sticker_packs
  for each row execute function public.yory_set_updated_at();

create or replace function public.yory_handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, display_name)
  values (
    new.id,
    left(
      coalesce(
        new.raw_user_meta_data ->> 'display_name',
        new.raw_user_meta_data ->> 'name',
        ''
      ),
      60
    )
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists yory_on_auth_user_created on auth.users;
create trigger yory_on_auth_user_created
  after insert on auth.users
  for each row execute function public.yory_handle_new_user();

-- Explicit API grants. RLS still limits each row to the policies from migrations.
grant usage on schema public to anon, authenticated;

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
grant select, insert, update, delete on public.sticker_packs to authenticated;

grant select on storage.objects to anon, authenticated;
grant insert, update, delete on storage.objects to authenticated;

comment on function public.yory_handle_new_user() is
  'Creates a minimal profile row on signup; does not collect or store keyboard content.';
comment on constraint themes_theme_json_shape on public.themes is
  'Rejects theme documents that do not match the minimum YORY schema v1 structure.';
