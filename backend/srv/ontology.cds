using {mulino.work} from '../db/work';
using {mulino.responsibility} from '../db/responsibility';
using {mulino.evaluation} from '../db/evaluation';
using {mulino.runtime} from '../db/runtime';
using {mulino.commands} from '../db/commands';
using {mulino.identity} from '../db/identity';
using {mulino.definitions} from '../db/definitions';
using {mulino.governance} from '../db/policies';
using {mulino.inventory} from '../db/inventory';
using {mulino.evidence} from '../db/evidence';
using {mulino.work.read} from '../db/work-read';

/** Only explicit query actions are public; no default generic CRUD projections. */
@path: '/ontology'
service OntologyService {
  action query(operation : String, requestJson : LargeString) returns LargeString;
  action command(requestJson : LargeString) returns LargeString;
  action validateCommand(requestJson : LargeString) returns LargeString;
}
