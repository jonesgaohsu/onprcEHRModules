ALTER TABLE onprc_billing.grantProjects DROP COLUMN alias;
ALTER TABLE onprc_billing.grantProjects DROP COLUMN aliasEnabled;

CREATE TABLE onprc_billing.aliases (
  rowid serial,
  alias varchar(200),
  aliasEnabled Varchar(100),

  projectNumber varchar(200),
  grantNumber varchar(200),
  agencyAwardNumber varchar(200),
  investigatorId int,
  investigatorName varchar(200),
  fiscalAuthority int,

  container ENTITYID NOT NULL,
  createdBy USERID,
  created timestamp,
  modifiedBy USERID,
  modified timestamp,

  CONSTRAINT PK_aliases PRIMARY KEY (rowid)
);
