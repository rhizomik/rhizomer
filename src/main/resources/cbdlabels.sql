create procedure DB.DBA.SPARQL_DESC_DICT_CBDL (in subj_dict any, in consts any, in good_graphs any, in bad_graphs any, in storage_name any, in options any)
{
  declare all_subjs, phys_subjects, sorted_good_graphs, sorted_bad_graphs, next_iter_subjs, res, props_labelled any;
  declare uid, graphs_listed, g_ctr, good_g_count, bad_g_count, s_ctr, all_s_count, phys_s_count integer;
  declare gs_app_callback, gs_app_uid varchar;
  declare rdf_type_iid, rdfs_label_iid IRI_ID;
  uid := get_keyword ('uid', options, http_nobody_uid());
  gs_app_callback := get_keyword ('gs-app-callback', options);
  if (gs_app_callback is not null)
    gs_app_uid := get_keyword ('gs-app-uid', options);
  rdf_type_iid := iri_to_id (UNAME'http://www.w3.org/1999/02/22-rdf-syntax-ns#type');
  rdfs_label_iid := iri_to_id (UNAME'http://www.w3.org/2000/01/rdf-schema#label');
  res := dict_new ();
  if (isinteger (consts))
    return res;
  foreach (any c in consts) do
    {
      if (isiri_id (c))
        dict_put (subj_dict, c, 0);
    }
  all_subjs := dict_list_keys (subj_dict, 0);
  next_iter_subjs := dict_new ();
  props_labelled := dict_new ();
  all_s_count := length (all_subjs);
  if (0 = all_s_count)
    return res;

next_iteration:
  all_s_count := length (all_subjs);
  gvector_sort (all_subjs, 1, 0, 0);
  -- dbg_obj_princ ('new iteration: all_subjs = ', all_subjs);
  if (__tag of integer = __tag (good_graphs))
    graphs_listed := 0;
  else
    {
      vectorbld_init (sorted_good_graphs);
      foreach (any g in good_graphs) do
        {
          if (isiri_id (g) and g < min_bnode_iri_id () and
            __rgs_ack_cbk (g, uid, 1) and
            (gs_app_callback is null or bit_and (1, call (gs_app_callback) (g, gs_app_uid))) )
            vectorbld_acc (sorted_good_graphs, g);
        }
      vectorbld_final (sorted_good_graphs);
      good_g_count := length (sorted_good_graphs);
      if (0 = good_g_count)
        return res;
      graphs_listed := 1;
    }
  vectorbld_init (sorted_bad_graphs);
  foreach (any g in bad_graphs) do
    {
      if (isiri_id (g) and g < min_bnode_iri_id ())
        vectorbld_acc (sorted_bad_graphs, g);
    }
  vectorbld_final (sorted_bad_graphs);
  bad_g_count := length (sorted_bad_graphs);
  vectorbld_init (phys_subjects);
  if (isinteger (storage_name))
    storage_name := 'http://www.openlinksw.com/schemas/virtrdf#DefaultQuadStorage';
  else if ('' = storage_name)
    {
      for (s_ctr := 0; s_ctr < all_s_count; s_ctr := s_ctr + 1)
        {
          declare s, phys_s any;
          s := all_subjs [s_ctr];
          if (isiri_id (s))
            vectorbld_acc (phys_subjects, s);
          else
            {
              phys_s := iri_to_id (s, 0, 0);
              if (not isinteger (phys_s))
                vectorbld_acc (phys_subjects, phys_s);
            }
        }
      vectorbld_final (phys_subjects);
      goto describe_physical_subjects;
    }
  -- dbg_obj_princ ('storage_name=',storage_name, ' sorted_good_graphs=', sorted_good_graphs, ' sorted_bad_graphs=', sorted_bad_graphs);
  for (s_ctr := 0; s_ctr < all_s_count; s_ctr := s_ctr + 1)
    {
      declare s, phys_s, maps any;
      declare maps_len integer;
      s := all_subjs [s_ctr];
      maps := sparql_quad_maps_for_quad (NULL, s, NULL, NULL, storage_name, case (graphs_listed) when 0 then vector() else sorted_good_graphs end, sorted_bad_graphs);
      -- dbg_obj_princ ('s = ', s, id_to_iri (s), ' maps = ', maps);
      maps_len := length (maps);
      if ((maps_len > 0) and (maps[maps_len-1][0] = UNAME'http://www.openlinksw.com/schemas/virtrdf#DefaultQuadMap'))
        {
          if (isiri_id (s))
            {
              phys_s := s;
              vectorbld_acc (phys_subjects, phys_s);
            }
          else
            {
              phys_s := iri_to_id (s, 0, 0);
              if (not isinteger (phys_s))
                vectorbld_acc (phys_subjects, phys_s);
            }
          maps := subseq (maps, 0, maps_len-1);
          maps_len := maps_len - 1;
        }
      if (maps_len > 0)
        all_subjs [s_ctr] := vector (s, maps);
      else
        all_subjs [s_ctr] := 0;
      -- dbg_obj_princ ('s = ', s, ' maps = ', maps);
      -- dbg_obj_princ ('all_subjs [', s_ctr, '] = ', all_subjs [s_ctr]);
    }
  vectorbld_final (phys_subjects);
  for (s_ctr := 0; s_ctr < all_s_count; s_ctr := s_ctr + 1)
    {
      declare s_desc, s, maps any;
      declare map_ctr, maps_len integer;
      declare fname varchar;
      s_desc := all_subjs [s_ctr];
      if (isinteger (s_desc))
        goto end_of_s;
      s := s_desc[0];
      maps := s_desc[1];
      maps_len := length (maps);
      fname := sprintf ('SPARQL_DESC_DICT_CBDL_QMV1_%U', md5 (storage_name || cast (graphs_listed as varchar) || md5_box (maps) || md5_box (sorted_bad_graphs)));
      if (not exists (select top 1 1 from Db.DBA.SYS_PROCEDURES where P_NAME = 'DB.DBA.' || fname))
        {
          declare ses, txt, saved_user any;
          ses := string_output ();
          http ('create procedure DB.DBA."' || fname || '" (in subj any, inout subj_dict any, inout next_iter_subjs any, inout res any', ses);
          if (graphs_listed)
            http (', inout sorted_good_graphs any', ses);
          http (')\n', ses);
          http ('{\n', ses);
          http ('  declare subj_iri varchar;\n', ses);
          http ('  subj_iri := id_to_iri_nosignal (subj);\n', ses);
          http ('  for (sparql define output:valmode "LONG" define input:storage <' || storage_name || '> ', ses);
          foreach (any g in sorted_bad_graphs) do
            {
              http ('  define input:named-graph-exclude <' || id_to_iri_nosignal (g) || '>\n', ses);
            }
          http ('select ?g1 ?p1 ?o1 ?g2 ?st2\n', ses);
          http ('      where { graph ?g1 {\n', ses);
          for (map_ctr := 0; map_ctr < maps_len; map_ctr := map_ctr + 1)
            {
              if (map_ctr > 0) http ('              union\n', ses);
              http ('              { quad map <' || maps[map_ctr][0] || '> { ?:subj_iri ?p1 ?o1 } }\n', ses);
            }
          http ('            }\n', ses);
          http ('          optional { graph ?g2 {\n', ses);
          http ('                  ?st2 a rdf:Statement ; rdf:subject ?:subj_iri ; rdf:predicate ?p1 ; rdf:object ?o1 } }\n', ses);
          http (' } ) do { ', ses);
          http ('            } } ) do { ', ses);
          if (graphs_listed)
            http ('      if (position ("g1", sorted_good_graphs)) {\n', ses);
          http ('      dict_put (res, vector (subj, "p1", "o1"), 1);\n', ses);
          http ('      if (isiri_id ("o1") and "o1" > min_bnode_iri_id() and dict_get (subj_dict, "o1") is null)\n', ses);
          http ('        dict_put (next_iter_subjs, "o1", 1);\n', ses);
          if (graphs_listed)
            http ('      if (position ("g2", sorted_good_graphs)) {\n', ses);
          http ('      if ("st2" is not null and dict_get (subj_dict, "st2") is null)\n', ses);
          http ('        dict_put (next_iter_subjs, "o1", 1);\n', ses);
          if (graphs_listed)
            http ('        } }\n', ses);
          http ('      } } }\n', ses);
          txt := string_output_string (ses);
          -- dbg_obj_princ ('Procedure text: ', txt);
	  saved_user := user;
	  set_user_id ('dba', 1);
          exec (txt);
	  set_user_id (saved_user);
        }
      if (graphs_listed)
        {
          -- dbg_obj_princ ('call (''DB.DBA.', fname, ''')(', s, subj_dict, next_iter_subjs, res, sorted_good_graphs, ')');
          call ('DB.DBA.' || fname)(s, subj_dict, next_iter_subjs, res, sorted_good_graphs);
        }
      else
        {
          -- dbg_obj_princ ('call (''DB.DBA.', fname, ''')(', s, subj_dict, next_iter_subjs, res, ')');
          call ('DB.DBA.' || fname)(s, subj_dict, next_iter_subjs, res);
        }
end_of_s: ;
    }

describe_physical_subjects:
  gvector_sort (phys_subjects, 1, 0, 0);
  phys_s_count := length (phys_subjects);
  -- dbg_obj_princ ('phys_subjects = ', phys_subjects);
  if (0 = phys_s_count)
    return res;
  -- dbg_obj_princ ('sorted_bad_graphs = ', sorted_bad_graphs);
  if (graphs_listed)
    {
      gvector_sort (sorted_good_graphs, 1, 0, 0);
      -- dbg_obj_princ ('sorted_good_graphs = ', sorted_good_graphs);
      for (g_ctr := good_g_count - 1; g_ctr >= 0; g_ctr := g_ctr - 1)
        {
          declare graph any;
          graph := sorted_good_graphs [g_ctr];
          for (s_ctr := phys_s_count - 1; s_ctr >= 0; s_ctr := s_ctr - 1)
            {
              declare subj any;
              subj := phys_subjects [s_ctr];
              for (select P as p1, O as obj1 from DB.DBA.RDF_QUAD where G = graph and S = subj) do
                {
                  -- dbg_obj_princ ('found3 ', subj, p1, ' in ', graph);
                  dict_put (res, vector (subj, p1, __rdf_long_of_obj (obj1)), 0);
                  if (isiri_id (obj1) and obj1 > min_bnode_iri_id() and dict_get (subj_dict, obj1) is null)
                    dict_put (next_iter_subjs, obj1, 1);
                  else
                    {
                      if (isiri_id (obj1) and dict_get (subj_dict, obj1) is null)
                        {
                          foreach (any g in sorted_good_graphs) do
					  	  {
                            for (select O as label1 from DB.DBA.RDF_QUAD where G = g and S = obj1 and P = rdfs_label_iid) do
                		      {
                		      	dict_put (res, vector (obj1, rdfs_label_iid, __rdf_long_of_obj (label1)), 0);
                		      }
                		  }
                        }
                    }
                  if (dict_get (props_labelled, p1) is null)
                    {
                      foreach (any g in sorted_good_graphs) do
					  {
					    for (select O as label1 from DB.DBA.RDF_QUAD where G = g and S = p1 and P = rdfs_label_iid) do
                		  {
                		  	dict_put (res, vector (p1, rdfs_label_iid, __rdf_long_of_obj (label1)), 0);
                		  }
					  }
                	  dict_put (props_labelled, p1, 1);
                	}
                  for (sparql define output:valmode "LONG"
                    select ?g2 ?st2 where {
                        graph ?g2 {
                            ?st2 a rdf:Statement ; rdf:subject ?:subj ; rdf:predicate ?:p1 ; rdf:object ?:obj1 } } ) do
                    {
                      if (position ("g2", sorted_good_graphs) and dict_get (subj_dict, "st2") is null)
                        dict_put (next_iter_subjs, st2, 1);
                    }
                }
            }
        }
    }
  else
    {
      for (s_ctr := phys_s_count - 1; s_ctr >= 0; s_ctr := s_ctr - 1)
        {
          declare subj any;
          subj := phys_subjects [s_ctr];
          for (select P as p1, O as obj1 from DB.DBA.RDF_QUAD where
            0 = position (G, sorted_bad_graphs) and
            S = subj and
            __rgs_ack_cbk (G, uid, 1) and
            (gs_app_callback is null or bit_and (1, call (gs_app_callback) (G, gs_app_uid))) ) do
            {
              -- dbg_obj_princ ('found4 ', subj, p1);
              dict_put (res, vector (subj, p1, __rdf_long_of_obj (obj1)), 0);
              if (isiri_id (obj1) and obj1 > min_bnode_iri_id() and dict_get (subj_dict, obj1) is null)
                dict_put (next_iter_subjs, obj1, 1);
              else
                {
                  if (isiri_id (obj1) and dict_get (subj_dict, obj1) is null)
                    {
                      for (select O as label1 from DB.DBA.RDF_QUAD where
            			0 = position (G, sorted_bad_graphs) and
            			S = obj1 and P = rdfs_label_iid and
            			__rgs_ack_cbk (G, uid, 1) and
            			(gs_app_callback is null or bit_and (1, call (gs_app_callback) (G, gs_app_uid))) ) do
            			{
              		      dict_put (res, vector (obj1, rdfs_label_iid, __rdf_long_of_obj (label1)), 0);
              		    }
                    }
                }
              if (dict_get (props_labelled, p1) is null)
                {
	              for (select O as labelP from DB.DBA.RDF_QUAD where
	            	0 = position (G, sorted_bad_graphs) and
	            	S = p1 and P = rdfs_label_iid and
	            	__rgs_ack_cbk (G, uid, 1) and
	            	(gs_app_callback is null or bit_and (1, call (gs_app_callback) (G, gs_app_uid))) ) do
	            	{
	              		dict_put (res, vector (p1, rdfs_label_iid, __rdf_long_of_obj (labelP)), 0);
	              	}
                  dict_put (props_labelled, p1, 1);
                }
              for (sparql define output:valmode "LONG"
                select ?g2 ?st2 where {
                    graph ?g2 {
                        ?st2 a rdf:Statement ; rdf:subject ?:subj ; rdf:predicate ?:p1 ; rdf:object ?:obj1 } } ) do
                {
                  if (0 = position ("g2", sorted_bad_graphs) and
                    dict_get (subj_dict, "st2") is null and
                    __rgs_ack_cbk ("g2", uid, 1) and
                    (gs_app_callback is null or bit_and (1, call (gs_app_callback) ("g2", gs_app_uid))) )
                    dict_put (next_iter_subjs, st2, 1);
                }
            }
        }
    }
ret_or_next_iter:
  if (0 = dict_size (next_iter_subjs))
    {
      -- dbg_obj_princ ('no new subjs, res = ', dict_list_keys (res, 0));
      return res;
    }
  all_subjs := dict_list_keys (next_iter_subjs, 1);
  foreach (IRI_ID s in all_subjs) do dict_put (subj_dict, s, 1);
  goto next_iteration;
}
;

create procedure DB.DBA.SPARQL_DESC_DICT_CBDL_PHYSICAL (in subj_dict any, in consts any, in good_graphs any, in bad_graphs any, in storage_name any, in options any)
{
  declare all_subjs, sorted_good_graphs, sorted_bad_graphs, next_iter_subjs, res, props_labelled any;
  declare uid, graphs_listed, g_ctr, good_g_count, bad_g_count, s_ctr, all_s_count integer;
  declare gs_app_callback, gs_app_uid varchar;
  declare rdf_type_iid, rdfs_label_iid IRI_ID;
  uid := get_keyword ('uid', options, http_nobody_uid());
  gs_app_callback := get_keyword ('gs-app-callback', options);
  if (gs_app_callback is not null)
    gs_app_uid := get_keyword ('gs-app-uid', options);
  rdf_type_iid := iri_to_id (UNAME'http://www.w3.org/1999/02/22-rdf-syntax-ns#type');
  rdfs_label_iid := iri_to_id (UNAME'http://www.w3.org/2000/01/rdf-schema#label');
  res := dict_new ();
  if (isinteger (consts))
    return res;
  foreach (any c in consts) do
    {
      if (isiri_id (c))
        dict_put (subj_dict, c, 0);
    }
  all_subjs := dict_list_keys (subj_dict, 0);
  next_iter_subjs := dict_new ();
  props_labelled := dict_new ();
  all_s_count := length (all_subjs);
  if (0 = all_s_count)
    return res;

next_iteration:
  all_s_count := length (all_subjs);
  gvector_sort (all_subjs, 1, 0, 0);
  -- dbg_obj_princ ('new iteration: all_subjs = ', all_subjs);
  if (__tag of integer = __tag (good_graphs))
    graphs_listed := 0;
  else
    {
      vectorbld_init (sorted_good_graphs);
      foreach (any g in good_graphs) do
        {
          if (isiri_id (g) and g < min_bnode_iri_id () and
            __rgs_ack_cbk (g, uid, 1) and
            (gs_app_callback is null or bit_and (1, call (gs_app_callback) (g, gs_app_uid))) )
            vectorbld_acc (sorted_good_graphs, g);
        }
      vectorbld_final (sorted_good_graphs);
      good_g_count := length (sorted_good_graphs);
      if (0 = good_g_count)
        return res;
      graphs_listed := 1;
    }
  vectorbld_init (sorted_bad_graphs);
  foreach (any g in bad_graphs) do
    {
      if (isiri_id (g) and g < min_bnode_iri_id ())
        vectorbld_acc (sorted_bad_graphs, g);
    }
  vectorbld_final (sorted_bad_graphs);
  bad_g_count := length (sorted_bad_graphs);
  -- dbg_obj_princ ('all_subjs = ', all_subjs);
  if (0 = all_s_count)
    return res;
  -- dbg_obj_princ ('sorted_bad_graphs = ', sorted_bad_graphs);
  if (graphs_listed)
    {
      gvector_sort (sorted_good_graphs, 1, 0, 0);
      -- dbg_obj_princ ('sorted_good_graphs = ', sorted_good_graphs);
      for (g_ctr := good_g_count - 1; g_ctr >= 0; g_ctr := g_ctr - 1)
        {
          declare graph any;
          graph := sorted_good_graphs [g_ctr];
          for (s_ctr := all_s_count - 1; s_ctr >= 0; s_ctr := s_ctr - 1)
            {
              declare subj any;
              subj := all_subjs [s_ctr];
              for (select P as p1, O as obj1 from DB.DBA.RDF_QUAD where G = graph and S = subj) do
                {
                  -- dbg_obj_princ ('found3 ', subj, p1, ' in ', graph);
                  dict_put (res, vector (subj, p1, __rdf_long_of_obj (obj1)), 0);
                  if (isiri_id (obj1) and obj1 > min_bnode_iri_id() and dict_get (subj_dict, obj1) is null)
                    dict_put (next_iter_subjs, obj1, 1);
                  else
                    {
                      if (isiri_id (obj1) and dict_get (subj_dict, obj1) is null)
                        {
                          foreach (any g in sorted_good_graphs) do
					  	  {
                            for (select O as label1 from DB.DBA.RDF_QUAD where G = g and S = obj1 and P = rdfs_label_iid) do
                		      {
                		      	dict_put (res, vector (obj1, rdfs_label_iid, __rdf_long_of_obj (label1)), 0);
                		      }
                		  }
                        }
                    }
                  if (dict_get (props_labelled, p1) is null)
                    {
                      foreach (any g in sorted_good_graphs) do
					  {
					    for (select O as label1 from DB.DBA.RDF_QUAD where G = g and S = p1 and P = rdfs_label_iid) do
                		  {
                		  	dict_put (res, vector (p1, rdfs_label_iid, __rdf_long_of_obj (label1)), 0);
                		  }
					  }
                	  dict_put (props_labelled, p1, 1);
                	}
                  for (sparql define output:valmode "LONG"
                    select ?g2 ?st2 where {
                        graph ?g2 {
                            ?st2 a rdf:Statement ; rdf:subject ?:subj ; rdf:predicate ?:p1 ; rdf:object ?:obj1 } } ) do
                    {
                      if (position ("g2", sorted_good_graphs) and dict_get (subj_dict, "st2") is null)
                        dict_put (next_iter_subjs, st2, 1);
                    }
                }
            }
        }
    }
  else
    {
      for (s_ctr := all_s_count - 1; s_ctr >= 0; s_ctr := s_ctr - 1)
        {
          declare subj any;
          subj := all_subjs [s_ctr];
          for (select P as p1, O as obj1 from DB.DBA.RDF_QUAD where
            0 = position (G, sorted_bad_graphs) and
            S = subj and
            __rgs_ack_cbk (G, uid, 1) and
            (gs_app_callback is null or bit_and (1, call (gs_app_callback) (G, gs_app_uid))) ) do
            {
              -- dbg_obj_princ ('found4 ', subj, p1);
              dict_put (res, vector (subj, p1, __rdf_long_of_obj (obj1)), 0);
              if (isiri_id (obj1) and obj1 > min_bnode_iri_id() and dict_get (subj_dict, obj1) is null)
                dict_put (next_iter_subjs, obj1, 1);
              else
                {
                  if (isiri_id (obj1) and dict_get (subj_dict, obj1) is null)
                    {
                      for (select O as label1 from DB.DBA.RDF_QUAD where
            			0 = position (G, sorted_bad_graphs) and
            			S = obj1 and P = rdfs_label_iid and
            			__rgs_ack_cbk (G, uid, 1) and
            			(gs_app_callback is null or bit_and (1, call (gs_app_callback) (G, gs_app_uid))) ) do
            			{
              		      dict_put (res, vector (obj1, rdfs_label_iid, __rdf_long_of_obj (label1)), 0);
              		    }
                    }
                }
              if (dict_get (props_labelled, p1) is null)
                {
	              for (select O as labelP from DB.DBA.RDF_QUAD where
	            	0 = position (G, sorted_bad_graphs) and
	            	S = p1 and P = rdfs_label_iid and
	            	__rgs_ack_cbk (G, uid, 1) and
	            	(gs_app_callback is null or bit_and (1, call (gs_app_callback) (G, gs_app_uid))) ) do
	            	{
	              		dict_put (res, vector (p1, rdfs_label_iid, __rdf_long_of_obj (labelP)), 0);
	              	}
                  dict_put (props_labelled, p1, 1);
                }
              for (sparql define output:valmode "LONG"
                select ?g2 ?st2 where {
                    graph ?g2 {
                        ?st2 a rdf:Statement ; rdf:subject ?:subj ; rdf:predicate ?:p1 ; rdf:object ?:obj1 } } ) do
                {
                  if (0 = position ("g2", sorted_bad_graphs) and
                    dict_get (subj_dict, "st2") is null and
                    __rgs_ack_cbk ("g2", uid, 1) and
                    (gs_app_callback is null or bit_and (1, call (gs_app_callback) ("g2", gs_app_uid))) )
                    dict_put (next_iter_subjs, st2, 1);
                }
            }
        }
    }

ret_or_next_iter:
  if (0 = dict_size (next_iter_subjs))
    {
      -- dbg_obj_princ ('no new subjs, res = ', dict_list_keys (res, 0));
      return res;
    }
  all_subjs := dict_list_keys (next_iter_subjs, 1);
  foreach (IRI_ID s in all_subjs) do dict_put (subj_dict, s, 1);
  goto next_iteration;
}
;