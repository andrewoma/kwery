create table actor (
  id         integer identity,
  first_name varchar(255) not null,
  last_name  varchar(255) null,
  version    integer      not null
);

create table language (
  id      integer identity,
  name    varchar(255) not null,
  version integer      not null,
  constraint language_name_idx unique (name)
);

create table film (
  id                   integer identity,
  title                varchar(255)  not null,
  description          varchar(4000) not null,
  release_year         integer,
  language_id          integer       not null,
  original_language_id integer,
  length               integer,
  rating               varchar(255),
  special_features     varchar(255) array,
  version              integer       not null,
  constraint fk_film_language foreign key (language_id) references language (id),
  constraint fk_film_orig_language foreign key (original_language_id) references language (id)
);

create table film_actor (
  film_id  integer not null,
  actor_id integer not null,
  primary key (film_id, actor_id),
  constraint fk_fa_film foreign key (film_id) references film (id),
  constraint fk_fa_actor foreign key (actor_id) references actor (id)
)
