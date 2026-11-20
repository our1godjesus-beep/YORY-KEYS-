-- Harden the pre-existing automatic RLS event-trigger helper.
-- Keep the registered event trigger active, but remove direct client EXECUTE grants.
revoke execute on function public.rls_auto_enable() from public, anon, authenticated;

-- Verify the event trigger still applies RLS to newly-created public tables.
create table public.__yory_rls_event_trigger_probe (
  id integer primary key
);

do $verify$
begin
  if not exists (
    select 1
    from pg_class
    where oid = 'public.__yory_rls_event_trigger_probe'::regclass
      and relrowsecurity
  ) then
    raise exception 'The ensure_rls event trigger did not enable RLS after EXECUTE was revoked';
  end if;
end;
$verify$;

drop table public.__yory_rls_event_trigger_probe;
